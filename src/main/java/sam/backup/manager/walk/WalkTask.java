package sam.backup.manager.walk;

import static sam.backup.manager.UtilsFx.fx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;

import javafx.concurrent.Worker.State;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.myutils.Checker;
import sam.nopkg.Junk;

public class WalkTask implements Runnable {
	public static final Logger logger = Utils.getLogger(WalkTask.class); 

	private final Config config;
	private final WalkMode initialWalkMode;
	private final WalkListener listener;

	private final AtomicReference<State> state = new AtomicReference<>(null);
	private final AtomicReference<Thread> thread = new AtomicReference<>(null);
	private final AtomicBoolean cancel = new AtomicBoolean(false);
	private final FileTreeManager fmanager;

	private List<Path> exucludePaths;

	public WalkTask(Config config, WalkMode walkMode, FileTreeManager fmanager, WalkListener listener) {
		this.fmanager = fmanager;
		this.config = config;
		this.initialWalkMode = walkMode;
		this.listener = listener;
		setState(State.READY);
	}

	public List<Path> getExucludePaths() {
		return exucludePaths;
	}

	private void setState(State s) {
		state.set(s);
		fx(() -> listener.stateChange(s));
	}

	@Override
	public void run() {
		if(cancel.get())
			return;

		if(!state.compareAndSet(State.READY, State.RUNNING))
			throw new IllegalStateException("expected state: "+State.READY+", was: "+state.get());

		setState(State.RUNNING);
		thread.set(Thread.currentThread());
		FileTreeMeta ftm = null;
		List<Path> exucludePaths = new ArrayList<>();
		State state = null;

		try {
			List<FileTreeMeta> metas = config.getFileTreeMetas();

			for (int i = 0; i < metas.size(); i++) 
				call(ftm = metas.get(i), exucludePaths);
			
			state = State.SUCCEEDED;
		} catch (Throwable e) {
			if(cancel.get()) {
				state = State.CANCELLED;
			} else {
				String s = sourceWalkFailed ? "Source walk failed: "+src : "Target walk failed: "+target;
				failed(ftm, s, e);	
				state = State.FAILED;
			}
		} finally {
			thread.set(null);
			this.exucludePaths = exucludePaths;
			setState(state);
		}
	}

	boolean sourceWalkFailed = true;
	Path target = null, src = null;

	private void call(FileTreeMeta meta, List<Path> exucludePaths) throws IOException {
		FileTree filetree = meta.loadFiletree(fmanager, true); 

		sourceWalkFailed = true;
		boolean backupWalked = true;
		target = null;
		src = null;

		src = path(meta.getSource());

		if(Checker.notExists(src)) {
			failed(meta, "Source not found: "+src, null);
			return;
		} 

		if(initialWalkMode.isSource())
			walk(filetree, src, WalkMode.SOURCE, exucludePaths);

		sourceWalkFailed = false;
		target = path(meta.getTarget()); 

		if(config.getWalkConfig().walkBackup() 
				&& initialWalkMode.isBackup() 
				&& target != null 
				&& Files.exists(target)) {

			walk(filetree, target, WalkMode.BACKUP, exucludePaths);
			backupWalked = true;
		}

		new ProcessFileTree(fmanager, filetree, config, backupWalked)
		.run();
	}

	private Path path(PathWrap p) {
		return p == null ? null : p.path();
	}

	public void cancel() {
		if(!cancel.compareAndSet(false, true))
			return;

		if(state.get() == State.CANCELLED)
			return;

		Thread t = thread.getAndSet(null);
		if(t == null)
			setState(State.CANCELLED);
		else 
			t.interrupt();
	}

	private void failed(FileTreeMeta ftm, String msg, Throwable error) {
		fx(() -> listener.failed(ftm, msg, error));
	}
	private void walk(FileTree filetree, Path path, WalkMode w, List<Path> exucludePaths) throws IOException {
		new Walker(filetree, config, listener, path, w == WalkMode.SOURCE ? config.getSourceExcluder() : config.getTargetExcluder(), w, exucludePaths)
		.call();
	}
}