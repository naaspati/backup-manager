package sam.backup.manager.walk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.concurrent.Task;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.file.PathListToFileTree;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.inject.Injector;

//FIXME implement tasks
public class WalkTask extends Task<FileTree> {
	public static final Logger logger = LogManager.getLogger(WalkTask.class); 

	private final Config config;
	private FileTree rootTree;

	private final WalkMode initialWalkMode;
	private final WalkListener listener;
	private final Path source;
	private final Path target;
	private final Injector injector;

	public WalkTask(Injector injector, FileTree existing, Path source, Path target, Config config, WalkMode walkMode, WalkListener listener) {
		this.injector = injector;
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

		if(!exucludePaths.isEmpty() && Utils.isSaveExcludeList())
			Utils.saveInTempDirHideError(new PathListToFileTree(exucludePaths), config, "excluded", source.getFileName()+".txt");
		
		new ProcessFileTree(injector.instance(FileTreeManager.class), rootTree, config, backupWalked);
		return rootTree;
	}

	private Walker walker(WalkMode w, List<Path> exucludePaths) {
		if(w == WalkMode.SOURCE)
			return new Walker(rootTree, config, listener, source, config.getSourceFilter(), w, exucludePaths);
		else if(w == WalkMode.BACKUP) 
			return new Walker(rootTree, config, listener, target, config.getTargetFilter(), w, exucludePaths);
		else 
			throw new IllegalStateException("unknown walk mode: "+w);
	}
}