package sam.backup.manager.walk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.Drive;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.file.FileTree;

public class WalkTask implements Runnable {
	public static final Logger logger = LogManager.getLogger(WalkTask.class); 

	private final Config config;
	private final ICanceler canceler;
	private final FileTree rootTree;
	private final boolean skipModifiedCheck;

	private final WalkMode initialWalkMode;
	private final WalkListener listener;
	private final Walker walker;

	public WalkTask(Config config, WalkMode walkMode, ICanceler canceler, WalkListener listener) {
		this.config = config;
		this.rootTree = config.getFileTree();
		this.initialWalkMode = walkMode;
		this.listener = listener;

		this.skipModifiedCheck = config.isNoModifiedCheck();
		this.canceler = canceler;
		 walker = new Walker(config, listener, canceler);
	}

	private static final IdentityHashMap<FileTree, Void> sourceWalkCompleted = new IdentityHashMap<>();
	private static final IdentityHashMap<FileTree, Void> backupWalkCompleted = new IdentityHashMap<>();
	private boolean backupWalked;

	@Override
	public void run() {
		if(canceler.isCancelled())
			return;

		final Path root = config.getSource();

		if(Files.notExists(root)) {
			listener.walkFailed("Source not found: "+root, new FileNotFoundException("file not found: "+root));
			return;
		} 
		boolean sourceWalkFailed = true;

		try {
			if(initialWalkMode.isSource()) {
				if(!sourceWalkCompleted.containsKey(rootTree)) {
					walker.walk(root, config.getSourceFilter(), WalkMode.SOURCE);
					sourceWalkCompleted.put(rootTree, null);
				} else 
					logger.debug("source walk skipped: {}", root);
			}
			
			if(canceler.isCancelled())
				return; //TODO feed cancel event to listener

			sourceWalkFailed = false;

			boolean walk = !(config.isNoBackupWalk() || !Drive.exists());

			if(walk && initialWalkMode.isBackup()){
				if(!backupWalkCompleted.containsKey(rootTree)) {
					walker.walk(config.getTarget(), config.getTargetFilter(), WalkMode.BACKUP);
					backupWalkCompleted.put(rootTree, null);
				} else 
					logger.debug("backup walk skipped: {}", config.getTarget());

				backupWalked = true;
			}
		} catch (IOException e) {
			String s = sourceWalkFailed ? "Source walk failed: "+config.getSource() : "Target walk failed: "+config.getTarget();
			listener.walkFailed(s, e);
			logger.error(s, e);
			return;
		}

		rootTree.walkCompleted();
		new SaveExcludeFilesList(initialWalkMode, config, walker);
		new UpdateFileTree(config, skipModifiedCheck, backupWalked);

		listener.walkCompleted();
	}
}