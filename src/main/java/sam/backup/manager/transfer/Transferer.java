package sam.backup.manager.transfer;

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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.StoringMethod;
import sam.backup.manager.config.StoringSetting;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.State;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.file.FilteredDirEntity;
import sam.backup.manager.file.FilteredFileTree;
import sam.backup.manager.file.SimpleFileTreeWalker;
import sam.io.BufferSize;
import sam.myutils.MyUtilsBytes;
import sam.myutils.MyUtilsException;
import sam.reference.WeakList;

class Transferer implements Callable<State> {
	// https://en.wikipedia.org/wiki/Zip_(file_format)
	private static final long MAX_ZIP_SIZE = Integer.MAX_VALUE; // max is 4gb, but i am limit it to 2gb
	@SuppressWarnings("unused")
	private static final long MAX_ZIP_ENTRIES_COUNT = 65535;
	private static final long FILENAME_MAX = 260;

	public static final int BUFFER_SIZE = BufferSize.DEFAULT_BUFFER_SIZE;

	private static final WeakList<ByteBuffer> byteBuffers = new WeakList<>(true, () -> ByteBuffer.allocate(BUFFER_SIZE));
	private static final WeakList<byte[]> buffers = new WeakList<>(true, () -> new byte[BUFFER_SIZE]);

	private static final Logger LOGGER =  LoggerFactory.getLogger(Transferer.class);

	private final StoringSetting storingSetting;
	private final StoringMethod storingMethod;
	private final IFilter storingIncluder;
	private final List<Path> failed = new ArrayList<>();

	private final FilteredFileTree filesTree;
	private final ICanceler canceler;
	private final TransferListener listener;
	private final Set<Path> createdDirs = new HashSet<>();

	private volatile int filesCopied;
	private volatile long filesCopiedSize;

	private volatile int filesSelected;
	private volatile long filesSelectedSize;

	private volatile long currentBytesRead;
	private volatile long currentFileSize;

	private byte[] bytes;
	private List<FileTreeEntity> toBeRemoved;
	private final Config config;

	public Transferer(Config config, FilteredFileTree filesTree, ICanceler canceler, TransferListener listener) {
		this.config = config;
		this.filesTree = filesTree;
		this.canceler = canceler;
		this.listener = listener;
		this.storingSetting = config.getStoringMethod();
		this.storingMethod = storingSetting.getMethod();
		this.storingIncluder = storingSetting.getSelecter();
	}

	public int getFilesCopiedCount() { return filesCopied; }
	public long getFilesCopiedSize() { return filesCopiedSize; }
	public int getFilesSelectedCount() { return filesSelected; }
	public long getFilesSelectedSize() { return filesSelectedSize; }

	private final List<FileEntity> files = new ArrayList<>();
	private final List<FilteredDirEntity> zips = new ArrayList<>();
	private final Set<FilteredDirEntity> zipsCopied = new HashSet<>();

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
				if(ft.isCopied()) {
					long size = ft.getSourceSize();

					filesSelected++;
					filesSelectedSize += size;

					filesCopied++;
					filesCopiedSize += size;
				} else if (ft.isBackupable()) {
					filesSelected++;
					filesSelectedSize += ft.getSourceSize();
					files.add(ft);
				}
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult dir(DirEntity ft) {
				if(storingMethod != StoringMethod.ZIP)
					return FileVisitResult.CONTINUE;

				if(!ft.isBackupable())
					return FileVisitResult.SKIP_SUBTREE;

				if(storingMethod == StoringMethod.ZIP && storingIncluder.test(ft.getSourcePath())) {
					FilteredDirEntity fdir = (FilteredDirEntity) ft;
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
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}
		});

		save(zips, "zips-save");
		save(files, "files-save");
		if(!failed.isEmpty())
			writeInTempDir(config, "transfer-failed", null, failed.stream().map(String::valueOf).collect(Collectors.joining("\n")), LOGGER);
	}
	private <E extends FileTreeEntity> void save(List<E> files, String suffix) {
		writeInTempDir(config, "transfer-log-", suffix, new FileTreeString(filesTree, files), LOGGER);
	}
	private ByteBuffer buffer;

	@Override
	public State call() throws Exception {
		try {
			for (FilteredDirEntity d : zips) {
				if(zipDir(d) == CANCELLED)
					return CANCELLED;
			}
			State state = startCopy();
			if(toBeRemoved != null)
				toBeRemoved.forEach(FileTreeEntity::remove);
			filesTree.updateDirAttrs();
			return state;
		} finally {
			if(buffer != null)
				byteBuffers.add(buffer);
			if(bytes != null)
				buffers.add(bytes);
		}
	}

	public long getCurrentBytesRead() {
		return currentBytesRead;
	}
	public long getCurrentFileSize() {
		return currentFileSize;
	}
	private State startCopy() {
		for (FileEntity f : files) {
			if(canceler.isCancelled())
				return CANCELLED;

			if(!f.isBackupable())
				continue;

			newTask(f);
			copyStart(f);

			if(copy())
				f.setCopied(true);
			else
				failed.add(target);
			copyEnd(f);

		}
		return COMPLETED;
	} 

	private void copyStart(FileEntity f) {
		listener.copyStarted(src, target);
	}
	private void copyEnd(FileTreeEntity f) {
		filesCopied++;
		listener.copyCompleted(src, target);
	}
	private State zipDir(FilteredDirEntity fdir) {
		DirEntity dir = fdir.getDir();

		if(removeFromFileTree(dir))
			return State.COMPLETED;

		newTask(dir);
		if(dir.getSourceSize() > MAX_ZIP_SIZE)
			throw new RuntimeException(String.format("zipfile size (%s) exceeds max allows size (%s)", MyUtilsBytes.bytesToHumanReadableUnits(dir.getSourceSize(), false), MyUtilsBytes.bytesToHumanReadableUnits(MAX_ZIP_SIZE, false)));

		final int nameCount = dir.getSourcePath().getNameCount();
		target = dir.getBackupPath();

		if(target.toString().length() > FILENAME_MAX){
			LOGGER.error("FILENAME_MAX exceeded", new IOException("filepath length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+target));
			failed.add(target);
			return State.COMPLETED;
		}

		if(!createDir(dir.getBackupPath().getParent()))
			return State.COMPLETED;

		if(bytes == null)
			bytes = buffers.poll();

		boolean[] delete = {false};
		State[] state = {State.COMPLETED};
		IOException[] error = {null};
		Path tempfile = null;

		try {
			tempfile = Files.createTempFile(target.getFileName().toString(), null);

			try(OutputStream os = Files.newOutputStream(tempfile);
					ZipOutputStream zos = new ZipOutputStream(os);
					) {
				zos.setLevel(Deflater.BEST_SPEED);

				dir.walk(new SimpleFileTreeWalker() {
					@Override
					public FileVisitResult file(FileEntity ft) {
						if(removeFromFileTree(ft))
							return FileVisitResult.CONTINUE;

						if(canceler.isCancelled()) {
							cancel();
							return FileVisitResult.TERMINATE;
						}
						src = ft.getSourcePath();
						if(Files.notExists(src)) {
							LOGGER.warn("file not found: {}", src);
							return FileVisitResult.CONTINUE;
						}
						copyStart(ft);

						try {
							zipPipe(zos, nameCount);
						} catch (IOException e) {
							error[0] = e;
							return FileVisitResult.TERMINATE;
						}
						if(canceler.isCancelled()) {
							cancel();
							return FileVisitResult.TERMINATE;
						}
						copyEnd(fdir);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult dir(DirEntity ft) {
						if(removeFromFileTree(dir))
							return FileVisitResult.SKIP_SUBTREE;

						if(Files.notExists(ft.getSourcePath())) {
							LOGGER.warn("file not found: {}", src);
							return FileVisitResult.SKIP_SUBTREE;
						}
						return FileVisitResult.CONTINUE;
					}
					private void cancel() {
						delete[0] = true;
						state[0] = State.CANCELLED;
					}
				});
				if(error[0] != null)
					throw error[0]; 
			}
		} catch (Exception e) {
			failed.add(target);
			LOGGER.error("failed to zip dir: {}", nameCount, e);
			fdir.setCopied(false);
			delete[0] = true;
		} finally {
			if(delete[0]) delete(tempfile, "temp zip");
			else {
				move(tempfile, target.resolveSibling(target.getFileName()+".zip"), "zip file");
				fdir.setCopied(true);
				zipsCopied.add(fdir);
			}
		}
		return state[0];
	}

	private static final long ONE_MB = 1048576;

	private void move(Path src, Path target, String msg) {
		if(MyUtilsException.noError(() -> Files.size(src)) < ONE_MB)
			rename(src, target, "zip file");
		else {
			Path temp = siblingTemp(target);
			rename(src, temp, null);
			rename(temp, target, "zip file");	
		}
	}
	private static void rename(Path src, Path target, String msg) {
		try {
			Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
			if(msg != null)
				LOGGER.debug("file renamed: {} -> {}", src, target);
		} catch (IOException e) {
			LOGGER.error("failed to rename {}:{} -> {}", msg, src, target, e);
		}
	}
	private Path siblingTemp(Path p) {
		return p.resolveSibling(p.getFileName()+".tmp");
	}

	private boolean removeFromFileTree(FileTreeEntity dir) {
		if(dir.getSourceAttrs().getCurrent() == null) {
			LOGGER.debug("removed from filetree: {}", dir.getSourcePath());
			if(toBeRemoved == null)
				toBeRemoved = new ArrayList<>();
			toBeRemoved.add(dir);
			return true;
		}
		return false;

	}
	private void zipPipe(ZipOutputStream zos, int rootNameCount) throws IOException {
		boolean entryPut = false;
		int n = 0;

		try {
			String name = src.subpath(rootNameCount, src.getNameCount()).toString().replace('\\', '/');
			if(name.length() > FILENAME_MAX)
				throw new IOException("name length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+name);

			zos.putNextEntry(new ZipEntry(name));
			entryPut = true;

			try(InputStream strm = Files.newInputStream(src, READ);) {
				while((n = strm.read(bytes)) != -1) {
					if(canceler.isCancelled()) return;

					zos.write(bytes, 0, n);
					addBytesRead(n);
				}
			}
		} catch (IOException e) {
			LOGGER.error("failed to zip file: {}", src, e);
		} finally {
			if(entryPut)
				zos.closeEntry();
		}
	}
	private static void delete(Path p, String msg) {
		if(p == null) return;
		try {
			Files.deleteIfExists(p);
			LOGGER.debug("file deleted: {}", p);
		} catch (IOException e) {
			LOGGER.error("failed to delete {}:{}", msg, p, e);
		}
	}

	private Path src, target;

	private void newTask(FileTreeEntity f) {
		currentFileSize = f.getSourceSize();
		currentBytesRead = 0;

		src = f.getSourcePath();
		target = f.getBackupPath();
		listener.newTask();
	}

	private boolean copy() {
		if(canceler.isCancelled()) return false;

		if(target.toString().length() > FILENAME_MAX){
			LOGGER.error("FILENAME_MAX exceeded", new IOException("filepath length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+target));
			return false;
		}

		if(!createDir(target.getParent()))
			return false;

		if(canceler.isCancelled()) return false;

		if(buffer == null)
			buffer = byteBuffers.poll();

		buffer.clear();

		final Path temp = target.resolveSibling(target.getFileName()+".tmp");

		try(FileChannel in = FileChannel.open(src, READ);
				FileChannel out = FileChannel.open(temp, CREATE, TRUNCATE_EXISTING, WRITE)) {

			long size = in.size(); 

			if(size < BUFFER_SIZE) {
				in.transferTo(0, size, out);
				addBytesRead(in.size());
			} else {
				int n = 0;
				while((n = in.read(buffer)) != -1) {
					if(canceler.isCancelled()) return false;

					buffer.flip();
					out.write(buffer);
					buffer.clear();
					addBytesRead(n);
				}				
			}
			LOGGER.debug("file copied {} -> {}", src, temp);
		} catch (IOException e) {
			LOGGER.error("file copy failed {} -> {}", src, temp, e);
			delete(temp, "failed to copy file");
			return false;
		}
		rename(temp, target, "copied file");
		return true;
	}

	private boolean createDir(Path parent) {
		if(!createdDirs.contains(parent)) {
			try {
				Files.createDirectories(parent);
				createdDirs.add(parent);
			} catch (Exception e) {
				LOGGER.error("failed to create dir: {}", parent, e);
				return false;
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
