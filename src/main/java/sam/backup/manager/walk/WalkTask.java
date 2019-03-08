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

public class WalkTask implements Runnable {
	public static final Logger logger = Utils.getLogger(WalkTask.class); 

	private final Config config;
	private final WalkMode initialWalkMode;
	private final WalkListener listener;
	private final FileTreeMeta meta;
	private final FileTree fileTree;

	private final AtomicReference<State> state = new AtomicReference<>(null);
	private final AtomicReference<Thread> thread = new AtomicReference<>(null);
	private final AtomicBoolean cancel = new AtomicBoolean(false);
	private final FileTreeManager ftm;

	private List<Path> exucludePaths;

	public WalkTask(FileTreeMeta meta, Config config, WalkMode walkMode, FileTreeManager ftm, WalkListener listener) throws Exception {
		this.ftm = ftm;
		this.config = config;
		this.initialWalkMode = walkMode;
		this.listener = listener;
		this.meta = meta;
		
		this.fileTree = meta.getFileTree() == null ? meta.loadFiletree(ftm, true) : meta.getFileTree();
		
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

		exucludePaths = null;
		boolean sourceWalkFailed = true;
		boolean backupWalked = true;
		Path target = null, src = null;

		try {
			src = path(meta.getSource());

			if(Checker.notExists(src)) {
				failed("Source not found: "+src, null);
				return;
			} 

			List<Path> exucludePaths = new ArrayList<>();

			if(initialWalkMode.isSource())
				walk(src, WalkMode.SOURCE, exucludePaths);

			sourceWalkFailed = false;
			target = path(meta.getTarget()); 

			if(config.getWalkConfig().walkBackup() 
					&& initialWalkMode.isBackup() 
					&& target != null 
					&& Files.exists(target)) {

				walk(target, WalkMode.BACKUP, exucludePaths);
				backupWalked = true;
			}
			
			new ProcessFileTree(ftm, fileTree, config, backupWalked)
			.run();

			this.exucludePaths = exucludePaths;
			setState(State.SUCCEEDED);
		} catch (Throwable e) {
			if(cancel.get()) {
				setState(State.CANCELLED);
			} else {
				String s = sourceWalkFailed ? "Source walk failed: "+src : "Target walk failed: "+target;
				failed(s, e);	
			}
		} finally {
			thread.set(null);
		}
	}

	private Path path(PathWrap p) {
		return p == null ? null : p.path();
	}
	
	public void cancel() {
		if(!cancel.compareAndSet(false, true))
			return;
		if(state.get() != State.RUNNING)
			return;
		
		Thread t = thread.getAndSet(null);
		if(t == null)
			return;
		
		t.interrupt();
	}

	private void failed(String msg, Throwable error) {
		fx(() -> listener.failed(msg, null));
		setState(State.FAILED);
	}

	private void walk(Path path, WalkMode w, List<Path> exucludePaths) throws IOException {
		new Walker(fileTree, config, listener, path, w == WalkMode.SOURCE ? config.getSourceExcluder() : config.getTargetExcluder(), w, exucludePaths)
		.call();
	}
}