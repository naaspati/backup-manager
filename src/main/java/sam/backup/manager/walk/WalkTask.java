package sam.backup.manager.walk;

import static sam.backup.manager.UtilsFx.fx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;

import javafx.concurrent.Worker.State;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.FileTree;
import sam.myutils.Checker;

public class WalkTask implements Runnable {
	public static final Logger logger = Utils.getLogger(WalkTask.class); 

	private final Config config;
	private final WalkMode initialWalkMode;
	private final WalkListener listener;

	private final AtomicReference<State> state = new AtomicReference<>(null);
	private final AtomicReference<Thread> thread = new AtomicReference<>(null);
	private final AtomicBoolean cancel = new AtomicBoolean(false);
	private final FileTree filetree;
	private final FileTreeMeta meta;

	private List<Path> exucludePaths;

	public WalkTask(FileTreeMeta meta, Config config, WalkMode walkMode, WalkListener listener) {
		this.config = config;
		this.meta = meta;
		this.filetree = Objects.requireNonNull(meta.getFileTree()); 
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
		
		List<Path> exucludePaths = new ArrayList<>();
		State state = null;
		boolean backupWalked = true;
		boolean sourceWalkFailed = true;
		Path target = null, src = null;

		try {
			src = path(meta.getSource());

			if(Checker.notExists(src)) {
				failed("Source not found: "+src, null);
				return;
			} 

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

			new ProcessFileTree(filetree, config, backupWalked)
			.run();
			
			state = State.SUCCEEDED;
		} catch (Throwable e) {
			if(cancel.get()) {
				state = State.CANCELLED;
			} else {
				String s = sourceWalkFailed ? "Source walk failed: "+src : "Target walk failed: "+target;
				failed(s, e);	
				state = State.FAILED;
			}
		} finally {
			thread.set(null);
			this.exucludePaths = exucludePaths;
			setState(state);
		}
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

	private void failed(String msg, Throwable error) {
		fx(() -> listener.failed(msg, error));
	}
	private void walk(Path path, WalkMode w, List<Path> exucludePaths) throws IOException {
		new Walker(filetree, config, listener, path, w == WalkMode.SOURCE ? config.getSourceExcluder() : config.getTargetExcluder(), w, exucludePaths)
		.call();
	}
}