package sam.backup.manager.transfer;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.backup.manager.extra.State.CANCELLED;
import static sam.backup.manager.extra.State.COMPLETED;
import static sam.backup.manager.extra.Utils.writeInTempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.PathWrap;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.IFilter;
import sam.backup.manager.extra.State;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.backup.manager.file.api.FilteredDir;
import sam.backup.manager.file.api.FilteredDir;
import sam.myutils.MyUtilsBytes;
import sam.myutils.MyUtilsException;
import sam.myutils.MyUtilsPath;
import sam.nopkg.Junk;
import sam.reference.WeakPool;

class Transferer implements Callable<State> {
	private static final Logger LOGGER =  Utils.getLogger(Transferer.class);
	public static final int BUFFER_SIZE = Optional.ofNullable(System.getenv("BUFFER_SIZE")).map(Integer::parseInt).orElse(2*1024*1024);

	static {
		LOGGER.debug("BUFFER_SIZE: "+MyUtilsBytes.bytesToHumanReadableUnits(BUFFER_SIZE, false));
	}

	// https://en.wikipedia.org/wiki/Zip_(file_format)
	private static final long MAX_ZIP_SIZE = Integer.MAX_VALUE; // max is 4gb, but i am limit it to 2gb
	@SuppressWarnings("unused")
	private static final long MAX_ZIP_ENTRIES_COUNT = 65535;
	private static final long FILENAME_MAX = 260;

	private static final WeakPool<ByteBuffer> byteBuffers = new WeakPool<>(true, () -> ByteBuffer.allocate(BUFFER_SIZE));
	private static final WeakPool<byte[]> buffers = new WeakPool<>(true, () -> new byte[BUFFER_SIZE]);

	private final IFilter zipFilter;

	private final FilteredDir filesTree;
	private final TransferListener listener;
	private final Set<Path> createdDirs = new HashSet<>();

	private int filesCopied;
	private long filesCopiedSize;

	private int filesSelected;
	private long filesSelectedSize;

	private long currentBytesRead;
	private long currentFileSize;

	private List<FileEntity> toBeRemoved;
	private final Config config;

	public Transferer(Config config, FilteredDir filesTree, TransferListener listener) {
		this.config = config;
		this.filesTree = filesTree;
		this.listener = listener;
		this.zipFilter = config.getZipFilter();
	}

	public int getFilesCopiedCount() { return filesCopied; }
	public long getFilesCopiedSize() { return filesCopiedSize; }
	public int getFilesSelectedCount() { return filesSelected; }
	public long getFilesSelectedSize() { return filesSelectedSize; }

	private final List<FileEntity> files = new ArrayList<>();
	private final List<FilteredDir> zips = new ArrayList<>();
	private final Set<FilteredDir> zipsCopied = new HashSet<>();

	void update() {
		filesCopied = 0;
		filesCopiedSize = 0;

		filesSelected = 0;
		filesSelectedSize = 0;
		files.clear();
		zips.clear();

		filesTree.walk(new FileTreeWalker() {
			@Override
			public FileVisitResult file(FileEntity ft) {
				if(ft.getStatus().isCopied()) {
					long size = ft.getSourceSize();

					filesSelected++;
					filesSelectedSize += size;

					filesCopied++;
					filesCopiedSize += size;
				} else if (ft.getStatus().isBackupable()) {
					filesSelected++;
					filesSelectedSize += ft.getSourceSize();
					files.add(ft);
				}
				return CONTINUE;
			}
			@Override
			public FileVisitResult dir(Dir ft) {
				if(zipFilter == null)
					return CONTINUE;

				if(!ft.getStatus().isBackupable())
					return SKIP_SUBTREE;

				if(zipFilter.test(ft.getSourcePath().path())) {
					FilteredDir fdir = (FilteredDir) ft;
					long size = fdir.getDir().getSourceSize();
					int count = fdir.getDir().filesInTree();
					if(zipsCopied.contains(ft)) {
						filesCopied += count;
						filesCopiedSize += size;	
					} else {
						zips.add(fdir);
						filesSelected += count;
						filesSelectedSize += size;
					}
					return SKIP_SUBTREE;
				}
				return CONTINUE;
			}
		});

		save(zips, "zips-save");
		save(files, "files-save");
		if(failedPrinter != null && writer.getBuffer().length() != 0)
			writeInTempDir(config, "transfer-failed", null, writer.toString(), LOGGER);
	}
	private <E extends FileEntity> void save(List<E> files, String suffix) {
		writeInTempDir(config, "transfer-log-", suffix, new FileTreeString(filesTree, files), LOGGER);
	}
	@Override
	public State call() throws Exception {
		for (FilteredDir d : zips) {
			try {
				if(zipDir(d) == CANCELLED)
					return CANCELLED;
			} catch (IOException e) {
				LOGGER.error("Error: ", e);
				addFailed(d, e);
			}
		}
		State state = copyFiles();

		if(toBeRemoved != null)
			toBeRemoved.forEach(FileEntity::delete); //FIXME toBeRemoved.forEach(FileEntity::remove); 

		filesTree.updateDirAttrs();
		return state;
	}

	StringWriter writer;
	PrintWriter failedPrinter;

	private void addFailed(FileEntity d, Throwable e) {
		if(writer == null){
			writer = new StringWriter();
			failedPrinter = new PrintWriter(writer);
		}

		writer.append("source: ")
		.append(d.getSourcePath().toString())
		.append("\ntarget: ")
		.append(d.getBackupPath().toString())
		.append('\n');
	}

	public long getCurrentBytesRead() {
		return currentBytesRead;
	}
	public long getCurrentFileSize() {
		return currentFileSize;
	}
	private State copyFiles() {
		for (FileEntity f : files) {
			if(isCancelled())
				return CANCELLED;

			if(!f.getStatus().isBackupable())
				continue;

			PathWrap src = f.getSourcePath();
			PathWrap target = f.getBackupPath();
			newTask(f);
			copyStart(f);

			try {
				if(copy(src.path(), target.path()))
					f.getStatus().setCopied(true);
			} catch (IOException e) {
				LOGGER.error("file copy failed {} -> {}", src, target, e);
				addFailed(f, e);
			}
			copyEnd(f);

		}
		return COMPLETED;
	} 

	private void copyStart(FileEntity f) {
		listener.copyStarted(f.getSourcePath(), f.getBackupPath());
	}
	private void copyEnd(FileEntity f) {
		filesCopied++;
		listener.copyCompleted(f.getSourcePath(), f.getBackupPath());
	}
	private static final AtomicInteger counter = new AtomicInteger(0);

	private State zipDir(FilteredDir fdir) throws IOException {
		Dir dir = fdir.getDir();

		if(removeFromFileTree(dir))
			return COMPLETED;

		newTask(dir);
		if(dir.getSourceSize() > MAX_ZIP_SIZE)
			throw new IOException(String.format("zipfile size (%s) exceeds max allows size (%s)", MyUtilsBytes.bytesToHumanReadableUnits(dir.getSourceSize(), false), MyUtilsBytes.bytesToHumanReadableUnits(MAX_ZIP_SIZE, false)));

		final int nameCount = dir.getSourcePath().path().getNameCount();
		Path target = dir.getBackupPath().path();

		if(target.toString().length() > FILENAME_MAX)
			throw new IOException(new IOException("filepath length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+target));

		if(!createDir(dir.getBackupPath().path().getParent()))
			return COMPLETED;

		boolean[] delete = {false};
		State[] state = {COMPLETED};
		IOException[] error = {null};
		final Path tempfile = MyUtilsPath.TEMP_DIR.resolve(target.getFileName()+" - "+counter.incrementAndGet());

		try(OutputStream os = Files.newOutputStream(tempfile);
				ZipOutputStream zos = new ZipOutputStream(os); ) {
			zos.setLevel(Deflater.BEST_SPEED);

			dir.walk(new FileTreeWalker() {
				@Override
				public FileVisitResult file(FileEntity ft) {
					if(removeFromFileTree(ft))
						return CONTINUE;

					if(isCancelled()) {
						cancel();
						return TERMINATE;
					}
					Path src = ft.getSourcePath().path();
					if(Files.notExists(src)) {
						LOGGER.warn("file not found: {}", src);
						return CONTINUE;
					}
					copyStart(ft);

					try {
						zipPipe(src, zos, nameCount);
					} catch (IOException e) {
						error[0] = e;
						return TERMINATE;
					}
					if(isCancelled()) {
						cancel();
						return TERMINATE;
					}
					copyEnd(fdir);
					return CONTINUE;
				}

				@Override
				public FileVisitResult dir(Dir ft) {
					if(removeFromFileTree(dir))
						return SKIP_SUBTREE;

					if(Files.notExists(ft.getSourcePath().path())) {
						LOGGER.warn("file not found: {}", ft.getSourcePath());
						return SKIP_SUBTREE;
					}
					return CONTINUE;
				}
				private void cancel() {
					delete[0] = true;
					state[0] = CANCELLED;
				}
			});
			if(error[0] != null)
				throw error[0];
		} catch (IOException e) {
			fdir.getStatus().setCopied(false);
			delete[0] = true;
			throw e;
		} finally {
			if(delete[0]) 
				delete(tempfile, "temp zip");
		}

		
		if(!delete[0] && state[0] == COMPLETED)
			moveZip(fdir, tempfile);

		return state[0];
	}

	private void moveZip(FilteredDir fdir, Path src) throws IOException {
		PathWrap targetPW = fdir.getBackupPath();
		Path target = fdir.getBackupPath().path();
		target = target.resolveSibling(target.getFileName()+".zip");
		newTask(MyUtilsException.noError(() -> Files.size(src)), src, target);
		PathWrap src2 = fdir.getSourcePath();
		listener.copyStarted(src2, targetPW);
		if(copy(src, target)){
			fdir.getStatus().setCopied(true);
			zipsCopied.add(fdir);
			delete(src, null);
		}
		listener.copyCompleted(src2, targetPW);
	}

	private void rename(Path src, Path target, String msg) throws IOException {
		try {
			Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
			if(msg != null)
				LOGGER.debug("file renamed: {} -> {}", src, target);
		} catch (IOException e) {
			throw new IOException("failed to rename:"+msg+": "+src+" -> "+target, e);
		}
	}
	private boolean removeFromFileTree(FileEntity dir) {
		if(dir.getSourceAttrs().current() == null) {
			LOGGER.debug("removed from filetree: {}", dir.getSourcePath());
			if(toBeRemoved == null)
				toBeRemoved = new ArrayList<>();
			toBeRemoved.add(dir);
			return true;
		}
		return false;

	}
	private void zipPipe(Path src, ZipOutputStream zos, int rootNameCount) throws IOException {
		boolean entryPut = false;
		int n = 0;
		byte[] buffer = buffers.poll();

		try {
			String name = src.subpath(rootNameCount, src.getNameCount()).toString().replace('\\', '/');
			if(name.length() > FILENAME_MAX)
				throw new IOException("name length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+name);

			zos.putNextEntry(new ZipEntry(name));
			entryPut = true;

			try(InputStream strm = Files.newInputStream(src, READ);) {
				while((n = strm.read(buffer)) != -1) {
					if(isCancelled()) return;
					zos.write(buffer, 0, n);
					addBytesRead(n);
				}
			}
		} catch (IOException e) {
			throw new IOException("failed to zip file: "+ src, e);
		} finally {
			if(entryPut)
				zos.closeEntry();
			buffers.offer(buffer);
		}
	}
	private static void delete(Path p, String msg) throws IOException {
		if(p == null) return;
		try {
			Files.deleteIfExists(p);
			if(msg != null)
				LOGGER.debug("{}: {}", msg, p);
		} catch (IOException e) {
			throw new IOException("failed to delete: "+p, e);
		}
	}

	// private Path src, target;

	private Path newTask(FileEntity f) {
		return newTask(f.getSourceSize(), f.getSourcePath().path(), f.getBackupPath().path());
	}
	private Path newTask(long size, Path src, Path target) {
		currentFileSize = size;
		currentBytesRead = 0;

		listener.newTask();
		return target;
	}
	private boolean copy(Path src, Path target) throws IOException {
		if(isCancelled()) return false;

		if(target.toString().length() > FILENAME_MAX)
			throw new IOException("filepath length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+target);
		
		if(!createDir(target.getParent()))
			return false;

		if(isCancelled()) return false;

		ByteBuffer buffer = byteBuffers.poll();
		buffer.clear();

		boolean directCopy=true;
		Path temp = null;
		
		try(FileChannel in = FileChannel.open(src, READ);) {
			long size = in.size();
			directCopy = size < BUFFER_SIZE; 
			temp =  directCopy ? target : target.resolveSibling(target.getFileName()+".tmp");

			try(FileChannel out = FileChannel.open(temp, CREATE, TRUNCATE_EXISTING, WRITE)) {
				if(directCopy) {
					in.transferTo(0, size, out);
					addBytesRead(in.size());
				} else {
					int n = 0;
					while((n = in.read(buffer)) != -1) {
						if(isCancelled()) return false;

						buffer.flip();
						out.write(buffer);
						buffer.clear();
						addBytesRead(n);
					}				
				}
			}
			LOGGER.debug("file copied {} -> {}", src, temp);
		} catch (IOException e) {
			if(!directCopy)
				delete(temp, "failed to copy file");
			throw e;
		} finally {
			byteBuffers.offer(buffer);
		}

		if(!directCopy)
			rename(temp, target, "copied file");
		return true;
	}

	private boolean isCancelled() {
		return Junk.notYetImplemented();
		// FIXME Auto-generated method stub
	}

	private boolean createDir(Path parent) throws IOException {
		if(!createdDirs.contains(parent)) {
			try {
				Files.createDirectories(parent);
				createdDirs.add(parent);
				LOGGER.debug("DIR CREATED: {}", parent);
			} catch (Exception e) {
				throw new IOException("failed to create dir: "+ parent, e);
			}
		}
		return true;
	}
	private void addBytesRead(long bytes) {
		currentBytesRead +=  bytes;
		filesCopiedSize += bytes;
		listener.addBytesRead(bytes);
	}



}
