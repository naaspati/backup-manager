package sam.backup.manager.transfer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.backup.manager.extra.State.CANCELLED;
import static sam.backup.manager.extra.State.COMPLETED;

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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.StoringMethod;
import sam.backup.manager.config.StoringSetting;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.State;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.file.FilteredDirEntity;
import sam.backup.manager.file.FilteredFileTree;
import sam.backup.manager.file.SimpleFileTreeWalker;
import sam.myutils.MyUtils;
import sam.weak.WeakStore;

class Transferer implements Callable<State> {
	// https://en.wikipedia.org/wiki/Zip_(file_format)
	private static final long MAX_ZIP_SIZE = Integer.MAX_VALUE; // max is 4gb, but i am limit it to 2gb
	private static final long MAX_ZIP_ENTRIES_COUNT = 65535;
	
	public static final int BUFFER_SIZE = Optional.ofNullable(System.getenv("BUFFER_SIZE")).map(Integer::parseInt).orElse(2*1024*1024);

	private static final WeakStore<ByteBuffer> buffers = new WeakStore<>(() -> ByteBuffer.allocateDirect(BUFFER_SIZE), true);
	private static final WeakStore<byte[]> byteBuffers = new WeakStore<>(() -> new byte[BUFFER_SIZE], true);

	private static final Logger LOGGER =  LogManager.getLogger(Transferer.class);

	private final StoringSetting storingSetting;
	private final StoringMethod storingMethod;
	private final IFilter storingIncluder;

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

	public Transferer(Config config, FilteredFileTree filesTree, ICanceler canceler, TransferListener listener) {
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
		
		save(zips, "-zips-save.txt");
		save(files, "-files-save.txt");
	}
	private <E extends FileTreeEntity> void save(List<E> files, String ext) {
		Utils.writeInTempDir("transfer-log", filesTree.getSourcePath(), ext, new FileTreeString(filesTree, files), LOGGER);
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
				buffers.add(buffer);
			if(bytes != null)
				byteBuffers.add(bytes);
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
			throw new RuntimeException(String.format("zipfile size (%s) exceeds max allows size (%s)", MyUtils.bytesToHumanReadableUnits(dir.getSourceSize(), false), MyUtils.bytesToHumanReadableUnits(MAX_ZIP_SIZE, false)));
		
		boolean rename = false;
		final int nameCount = dir.getSourcePath().getNameCount();
		target = renamePath(dir.getBackupPath(), ".zip");

		if(Files.exists(target)) {
			rename = true;
			target = renamePath(dir.getBackupPath(), ".zip.tmp");
		}
		if(!createDir(target.getParent()))
			return State.COMPLETED;
		
		if(bytes == null)
			bytes = byteBuffers.poll();

		boolean[] delete = {false};
		State[] state = {State.COMPLETED};
		IOException[] error = {null};

		try {
			try(OutputStream os = Files.newOutputStream(target, WRITE, CREATE, TRUNCATE_EXISTING);
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
			} catch (IOException e) {
				LOGGER.error("failed to zip dir: {}", nameCount, e);
				fdir.setCopied(false);
				delete[0] = true;
			}
		} finally {
			if(delete[0]) delete(target, "temp zip");
			else {
				if(rename)
					rename(target, renamePath(dir.getBackupPath(), ".zip"), "zip file");

				fdir.setCopied(true);
				zipsCopied.add(fdir);
			}
		}
		return state[0];
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

	private static Path renamePath(Path p, String end) {
		return p.resolveSibling(p.getFileName()+end);
	}

	private static void rename(Path src, Path target, String msg) {
		try {
			Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.debug("file renamed: {} -> {}", src, target);
		} catch (IOException e) {
			LOGGER.error("failed to rename {}:{} -> {}", msg, src, target, e);
		}

	}

	private void zipPipe(ZipOutputStream zos, int rootNameCount) throws IOException {
		boolean entryPut = false;
		int n = 0;

		try {
			zos.putNextEntry(new ZipEntry(src.subpath(rootNameCount, src.getNameCount()).toString().replace('\\', '/')));
			entryPut = true;

			try(InputStream strm = Files.newInputStream(src, READ);) {
				while((n = strm.read(bytes)) != -1) {
					if(canceler.isCancelled()) return;

					zos.write(bytes, 0, n);
					addBytesRead(n);
				}
			}
		} catch (Exception e) {
			LOGGER.error("failed to zip file: {}", src, e);
		} finally {
			if(entryPut)
				zos.closeEntry();
		}
	}

	private static void delete(Path p, String msg) {
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

		if(!createDir(target.getParent()))
			return false;

		if(canceler.isCancelled()) return false;

		if(buffer == null)
			buffer = buffers.poll();

		buffer.clear();

		boolean rename = false;
		final Path temp;
		if(Files.exists(target)) {
			rename = true;
			temp = target.resolveSibling(target.getFileName()+".tmp");
		} else 
			temp = target;

		try(FileChannel in = FileChannel.open(src, READ);
				FileChannel out = FileChannel.open(temp, CREATE, TRUNCATE_EXISTING, WRITE)) {

			if(in.size() < BUFFER_SIZE) {
				in.transferTo(0, in.size(), out);
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
		if(rename)
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
