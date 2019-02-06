package sam.backup.manager.walk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.PathListToFileTree;

public class WalkTask implements Callable<FileTree> {
	public static final Logger logger = Utils.getLogger(WalkTask.class); 

	private final Config config;
	private FileTree rootTree;

	private final WalkMode initialWalkMode;
	private final WalkListener listener;
	private final Path source;
	private final Path target;

	public WalkTask(FileTree existing, Path source, Path target, Config config, WalkMode walkMode, WalkListener listener) {
		this.config = config;
		this.rootTree = existing;
		this.initialWalkMode = walkMode;
		this.listener = listener;
		this.source = source;
		this.target = target;
	}

	private static final IdentityHashMap<FileTree, Void> sourceWalkCompleted = new IdentityHashMap<>();
	private static final IdentityHashMap<FileTree, Void> backupWalkCompleted = new IdentityHashMap<>();
	private boolean backupWalked;

	@Override
	public FileTree call() throws IOException {
		if(Files.notExists(this.source)) 
			throw new FileNotFoundException("Source not found: "+this.source);
			
		boolean sourceWalkFailed = true;
		List<Path> exucludePaths = new ArrayList<>(); 

		try {
			if(initialWalkMode.isSource()) {
				if(!sourceWalkCompleted.containsKey(rootTree)) {
					this.rootTree = walker(WalkMode.SOURCE, exucludePaths).call();
					sourceWalkCompleted.put(rootTree, null);
				} else 
					logger.debug("source walk skipped: {}", this.source);
			}
			
			sourceWalkFailed = false;

			if(config.getWalkConfig().walkBackup() 
					&& initialWalkMode.isBackup() 
					&& target != null 
					&& Files.exists(target) ){
				if(!backupWalkCompleted.containsKey(rootTree)) {
					this.rootTree = walker(WalkMode.BACKUP, exucludePaths).call();
					backupWalkCompleted.put(rootTree, null);
				} else 
					logger.debug("backup walk skipped: {}", target);

				backupWalked = true;
			}
		} catch (IOException e) {
			String s = sourceWalkFailed ? "Source walk failed: "+config.getSource() : "Target walk failed: "+target;
			logger.error(s, e);
			throw new IOException(s, e);
		}

		rootTree.walkCompleted();
		if(!exucludePaths.isEmpty() && Utils.SAVE_EXCLUDE_LIST)
			Utils.saveInTempDirHideError(new PathListToFileTree(exucludePaths), config, "excluded", source.getFileName()+".txt");
		
		new ProcessFileTree(rootTree, config, backupWalked);
		return rootTree;
	}

	private Walker walker(WalkMode w, List<Path> exucludePaths) {
		if(w == WalkMode.SOURCE)
			return new Walker(config, listener, source, config.getSourceFilter(), w, exucludePaths);
		else if(w == WalkMode.BACKUP) 
			return new Walker(config, listener, target, config.getTargetFilter(), w, exucludePaths);
		else 
			throw new IllegalStateException("unknown walk mode: "+w);
	}
}