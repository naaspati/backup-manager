package sam.backup.manager.view.backup;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.notExists;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Collections.unmodifiableList;
import static javafx.application.Platform.isFxApplicationThread;
import static javafx.concurrent.Worker.State.READY;
import static javafx.concurrent.Worker.State.RUNNING;
import static javafx.concurrent.Worker.State.SCHEDULED;
import static sam.io.IOUtils.write;
import static sam.myutils.MyUtilsBytes.bytesToHumanReadableUnits;
import static sam.myutils.MyUtilsExtra.nullSafe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.Logger;

import javafx.concurrent.Worker.State;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.IFilter;
import sam.backup.manager.config.api.PathWrap;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.reference.WeakPool;

@SuppressWarnings("rawtypes")
class TransferTask {
	private static final Logger LOGGER =  Utils.getLogger(TransferTask.class);
	public static final int BUFFER_SIZE = Optional.ofNullable(System.getenv("BUFFER_SIZE")).map(Integer::parseInt).orElse(2*1024*1024);

	static {
		LOGGER.debug("BUFFER_SIZE: "+bytesToHumanReadableUnits(BUFFER_SIZE, false));
	}

	// https://en.wikipedia.org/wiki/Zip_(file_format)
	private static final long MAX_ZIP_SIZE = Integer.MAX_VALUE; // max is 4gb, but i am limit it to 2gb
	@SuppressWarnings("unused")
	private static final long MAX_ZIP_ENTRIES_COUNT = 65535;
	private static final long FILENAME_MAX = 260;

	private static final WeakPool<byte[]> buffers = new WeakPool<>(true, () -> new byte[BUFFER_SIZE]);

	private final IFilter zipFilter;
	private final Dir rootDir;
	private volatile TransferListener listener0;
	private final AtomicReference<FutureTask> task = new AtomicReference<>(null);
	private final AtomicReference<State> state = new AtomicReference<>(READY);
	private final FileTree filetree;

	public TransferTask(Config config, FileTree fileTree, Dir rootDir) {
		this.filetree = fileTree;
		this.zipFilter = config.getZipFilter();
		this.rootDir = rootDir;
		setState(listener0, READY);
	}

	public State getState() {
		return state.get();
	}
	private void setState(TransferListener listener, State s) {
		state.set(s);

		if(listener != null)
			listener.stateChanged(s);
	}
	private void ensureNotRunning() {
		if(!isFxApplicationThread())
			throw new IllegalStateException("not fx thread");

		if(getState() == RUNNING)
			throw new IllegalStateException("RUNNING");		
	}

	void setListener(TransferListener listener) {
		ensureNotRunning();
		this.listener0 = listener;
	}

	public synchronized void execute(Executor executor) {
		ensureNotRunning();
		FutureTask task = this.task.get();

		if(task != null && !task.isDone() && !task.isCancelled()) 
			throw new IllegalStateException(String.valueOf(getState()));

		task = new FutureTask<>(() -> {run() ; return null;});

		this.task.set(task);
		setState(listener0, SCHEDULED);
		executor.execute(task);
	}

	private static class Counts {
		int copied_count;
		long copied_size;

		int selected_count;
		long selected_size;

		long currentReadSize;
		long currentTotalSize;
	}

	private Counts counts;
	private TransferListener listener;
	private Set<Dir> createdDirs;
	private int removeFromFileTree;

	private void run() {
		if(isFxApplicationThread())
			throw new IllegalStateException("running in Fx thread");

		listener = this.listener0;

		if(listener == null) { // rare chance
			LOGGER.warn("TransferListener not set");
			listener = (TransferListener) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{TransferListener.class}, new InvocationHandler() {
				@Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable { return null; }
			});
		}

		setState(listener, RUNNING);
		counts = new Counts();
		List<FileEntity> filesList = new ArrayList<>();
		List<Dir> zips = new ArrayList<>();
		removeFromFileTree = 0;

		listener.generalEvent(Type.COUNTING);

		rootDir.walk(new FileTreeWalker() {
			@Override
			public FileVisitResult file(FileEntity ft) {
				if(remove(ft))
					return CONTINUE;	
				
				if(ft.getStatus().isCopied()) {
					long size = ft.getSourceSize();

					counts.selected_count++;
					counts.selected_size += size;

					counts.copied_count++;
					counts.copied_size+= size;
				} else if (ft.getStatus().isBackupable()) {
					counts.selected_count++;
					counts.selected_size += ft.getSourceSize();
					filesList.add(ft);
				}
				return CONTINUE;
			}
			@Override
			public FileVisitResult dir(Dir ft) {
				if(remove(ft) || zipFilter == null)
					return CONTINUE;

				if(!ft.getStatus().isBackupable())
					return SKIP_SUBTREE;

				if(zipFilter.test(ft.getSourcePath().path())) {
					Dir fdir = (Dir) ft;
					long size = fdir.getSourceSize();
					int count = fdir.countFilesInTree();

					if(ft.getStatus().isCopied()) {
						counts.copied_count += count;
						counts.copied_size+= size;	
					} else {
						zips.add(fdir);
						counts.selected_count += count;
						counts.selected_size += size;
					}
					return SKIP_SUBTREE;
				}
				return CONTINUE;
			}
		});

		listener.generalEvent(Type.ZIP, Type.WILL_BE, unmodifiableList(zips));
		listener.generalEvent(Type.COPY, Type.WILL_BE, unmodifiableList(filesList));
		byte[] buffer = buffers.poll();
		createdDirs = nullSafe(createdDirs, HashSet::new);

		try {
			zip(zips, buffer);
			files(filesList, buffer);
			setState(listener, State.SUCCEEDED);
		} catch (Exception e) {
			if(e instanceof InterruptedException)
				setState(listener, State.CANCELLED);
			else
				setState(listener, State.FAILED);
		} finally {
			buffers.add(buffer);
		}
	}
	
	private boolean remove(FileEntity ft) {
		if(ft.getSourceAttrs().current() == null) {
			removeFromFileTree++;
			return true;
		}
		return false;
	}

	private void files(List<FileEntity> files, byte[] buffer) {
		listener.generalEvent(Type.COPY, Type.START_OPERATION, null);
		ByteBuffer buf = ByteBuffer.wrap(buffer);
		buf.clear();

		for (FileEntity f : files) {
			if(!f.getStatus().isBackupable())
				continue;
			
			try {
				listener.start(Type.COPY, f);
				copy(f, buf);
				listener.success(Type.COPY, f);
			} catch (IOException e) {
				listener.failed(Type.COPY, f, e);
			}
		}
		listener.generalEvent(Type.COPY, Type.END_OPERATION, null);
	}

	private void zip(List<Dir> zips, byte[] buffer) {
		if(zips.isEmpty())
			return;

		listener.generalEvent(Type.ZIP, Type.START_OPERATION, null);

		for (Dir d : zips) {
			try {
				if(d.getStatus().isCopied())
					listener.completed(Type.ZIP, d);
				else {
					listener.start(Type.ZIP, d);
					zipDir(d, buffer);
					listener.success(Type.ZIP, d);
				}
			} catch (IOException e) {
				listener.failed(Type.ZIP, d, e);
			}
		}
		listener.generalEvent(Type.ZIP, Type.END_OPERATION, null);
	}

	private void setCopied(Dir d, boolean b) {
		d.walk(new FileTreeWalker() {
			@Override
			public FileVisitResult file(FileEntity ft) {
				ft.getStatus().setCopied(b);
				return CONTINUE;
			}
			@Override
			public FileVisitResult dir(Dir ft) {
				ft.getStatus().setCopied(b);
				return CONTINUE;
			}
		});
	}

	private void zipDir(Dir dir, byte[] buffer) throws IOException {
		if(dir.getSourceSize() > MAX_ZIP_SIZE)
			throw new FileEntityException(dir, String.format("zipfile size (%s) exceeds max allows size (%s)", bytesToHumanReadableUnits(dir.getSourceSize(), false), bytesToHumanReadableUnits(MAX_ZIP_SIZE, false)));

		final int nameCount = dir.getSourcePath().path().getNameCount();
		Path target = dir.getBackupPath().path();

		if(target.toString().length() > FILENAME_MAX)
			throw new FileEntityException(dir, new FileEntityException(dir, "filepath length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+target));

		createParentDir(dir);

		FileEntityException[] error = {null};
		final Path tempfile = ext(target, ".zip.tmp");
		boolean success = false;

		try(OutputStream os = newOutputStream(tempfile);
				ZipOutputStream zos = new ZipOutputStream(os); ) {
			zos.setLevel(Deflater.BEST_SPEED);

			dir.walk(new FileTreeWalker() {
				@Override
				public FileVisitResult file(FileEntity ft) {
					if(remove(ft))
						return CONTINUE;

					listener0.start(Type.ADD_TO_ZIP, ft);
					setCurrent(ft);

					try {
						zipPipe(ft, zos, nameCount, buffer);
					} catch (IOException e) {
						error[0] = new FileEntityException(ft, e);
						return TERMINATE;
					}
					listener0.success(Type.ADD_TO_ZIP, ft);
					return CONTINUE;
				}

				@Override
				public FileVisitResult dir(Dir ft) {
					if(remove(dir))
						return SKIP_SUBTREE;

					if(notExists(ft.getSourcePath().path())) 
						return SKIP_SUBTREE;
					return CONTINUE;
				}
			});

			if(error[0] != null)
				throw error[0];
			else
				success = true;
		} finally {
			if(success) {
				move(dir, tempfile, ext(target, ".zip"));
				setCopied(dir, true);
			} else {
				setCopied(dir, false);
				delete(dir, tempfile);
			}
		}
	}

	private Path ext(Path p, String ext) {
		return p.resolveSibling(p.getFileName()+ext);
	}

	private void move(FileEntity ft, Path src, Path target) throws IOException {
		try {
			Files.move(src, target, REPLACE_EXISTING);
			LOGGER.debug("move file: {} -> {}", src, target);
		} catch (IOException e) {
			throw new FileMoveException(ft, src, target, e);
		}
	}
	private void zipPipe(FileEntity f, ZipOutputStream zos, int rootNameCount, byte[] buffer) throws IOException {
		Path src = f.getSourcePath().path();
		if(notExists(src)) 
			throw new FileNotFoundException();

		String name = src.subpath(rootNameCount, src.getNameCount()).toString().replace('\\', '/');
		if(name.length() > FILENAME_MAX)
			throw new FileEntityException(f, "name length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+name);

		zos.putNextEntry(new ZipEntry(name));

		try(InputStream strm = newInputStream(src, READ);) {
			int n = 0;
			while((n = strm.read(buffer)) != -1) {
				zos.write(buffer, 0, n);
				addBytesRead(f, n);
			}
		}
	}
	private void setCurrent(FileEntity ft) {
		counts.currentTotalSize = ft.getSourceSize();
		counts.currentReadSize = 0;
		addBytesRead(ft, 0);
	}

	private static void delete(FileEntity ft, Path p) throws IOException {
		if(p == null) return;
		try {
			deleteIfExists(p);
			LOGGER.debug("delete file: {}", p);
		} catch (IOException e) {
			throw new FileEntityException(ft, "failed to delete: "+p, e);
		}
	}
	private void copy(FileEntity ft, ByteBuffer buffer) throws IOException {
		PathWrap srcPW = ft.getSourcePath();
		PathWrap targetW = ft.getBackupPath();
		
		if(targetW.string().length() > FILENAME_MAX)
			throw new FileEntityException(ft, "filepath length exceeds FILENAME_MAX ("+FILENAME_MAX+"): "+targetW.string());

		createParentDir(ft);
		buffer.clear();
		boolean success = false;
		boolean direct = false;
		Path target = null;

		try(FileChannel in = FileChannel.open(srcPW.path())) {
			long size = in.size();
			direct = size < buffer.capacity();
			target = direct ? targetW.path() : ext(targetW.path(), ".temp");
			setCurrent(ft);
			
			if(ft.getSourceSize() != size)
				LOGGER.error("ft.getSourceSize({}) != size({})", ft.getSourceSize(), size);

			while(buffer.hasRemaining() && in.read(buffer) != -1) { }
			
			buffer.flip();
			if(direct && buffer.remaining() != size)
				throw new IOException(String.format("expected bytes: %s, was: %s", size, buffer.remaining()));
			
			try(FileChannel out = FileChannel.open(target, CREATE, TRUNCATE_EXISTING, WRITE)) {
				addBytesRead(ft, write(buffer, out, false));
				
				if(!direct) {
					while(in.read(buffer) != -1) 
						addBytesRead(ft, write(buffer, out, true));		
				}
				success = true;	
			}
		} finally {
			if(!success && !direct)
				delete(ft, target);
		}
		
		if(success) {
			if(!direct)
				move(ft, target, targetW.path());
			
			ft.getStatus().setCopied(true);
		}
	}

	private void addBytesRead(FileEntity ft, long bytes) {
		counts.currentReadSize += bytes;
		counts.copied_size += bytes;

		listener.subProgress(ft, counts.currentReadSize, counts.currentTotalSize);
			listener.totalProgress(counts.copied_size, counts.selected_size);
	}

	private void createParentDir(FileEntity ft) throws IOException {
		Path p = null;
		try {
			Dir d = ft.getParent();
			if(!createdDirs.contains(d)) {
				FileEntity f =  d == null ? ft : d;
				p = f.getBackupPath().path();

				if(d == null)
					p = p.getParent();

				createDirectories(p);
			}
		} catch (IOException e) {
			throw new DirCreationFailedException(p, ft, e);
		}
	}

	public FileTree getFileTree() {
		return filetree;
	}
}
