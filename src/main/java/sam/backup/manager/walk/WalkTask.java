package sam.backup.manager.walk;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.file.AboutFile;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeWalker;

public 	class WalkTask implements Runnable, FileVisitor<Path> {
	private final ConfigView configView;
	private final ListingView listView;
	private final Config config;
	private volatile  long sourceSize, targetSize;
	private volatile int sourceFileCount, sourceDirCount, targetFileCount, targetDirCount;
	private final boolean listWalk;
	private final ICanceler canceler;
	private final List<Path> excludeFilesList;
	private final FileTree rootTree;
	private WalkType walkType;
	private final Set<SkipBackupOption> skipOptions;

	private Predicate<Path> excluder;
	private int nameCount;

	private  WalkTask(ConfigView configView, ListingView listView, List<Path> excludeFilesList, WalkType walkType) {
		config = configView != null ? configView.getConfig() : listView.getConfig();
		this.rootTree = config.getFileTree();
		this.excludeFilesList = excludeFilesList;
		skipOptions = config.getBackupSkips();
		this.walkType = walkType;
		this.configView = configView;
		this.listView = listView;
		this.canceler = configView == null ? listView : configView;
		this.listWalk = listView != null;
	}
	public WalkTask(ConfigView configView, List<Path> excludeFilesList, WalkType walkType) {
		this(configView, null, excludeFilesList, walkType);
	}
	public WalkTask(ListingView lv) {
		this(null, lv, null, WalkType.LIST);
	}
	@Override
	public void run() {
		if(canceler.isCancelled())
			return;

		final Path root = config.getSource();

		if(Files.notExists(root)) {
			if(!listWalk)
				configView.finish("Source not found", true);
			return;
		}

		boolean sourceWalkFailed = true;

		try {
			if(!config.isSourceWalkCompleted())
				walk(root, config.getSourceExcluder(), walkType);
			if(listWalk) {
				listView.updateFileTree();
				return;
			}
			config.setSourceWalkCompleted(true);
			sourceWalkFailed = false;

			if(config.getTargetPath() != null && !config.isNoBackupWalk()) 
				walk(config.getTargetPath(), config.getTargetExcluder(), WalkType.BACKUP);
		} catch (IOException e) {
			String s = sourceWalkFailed ? "Source walk failed: "+config.getSource() : "Target walk failed: "+config.getTargetPath();
			runLater(() -> configView.setError(s, e));
			return;
		}

		if(!config.isNoDriveMode())
			rootTree.calculateTargetPath(config);
		updateBackups();
	}
	private void updateBackups() {
		boolean backupWalked = !(config.isNoBackupWalk() || config.isNoDriveMode());
		boolean onlyExistsCheck = skipOptions.contains(SkipBackupOption.FILE_EXISTS);

		List<FileTree> list = new ArrayList<>();

		rootTree.walk(new FileTreeWalker() {
			@Override
			public FileVisitResult file(FileTree ft, AboutFile source, AboutFile backup) {
				boolean backupNeeded = false;

				String reason = null;

				if(source != null) {
					// if backup is walked and file not found then backup-needed 
					backupNeeded = backupWalked && backup == null;
					if(backupNeeded) reason = "File not found in backup";
					// if !onlyExistsCheck then check modified time;
					backupNeeded = backupNeeded || (!onlyExistsCheck && source.modifiedTime != ft.getModifiedTime());
					if(backupNeeded) reason = reason != null ? reason : "File Modified";
				}

				ft.setBackupNeeded(backupNeeded, reason);
				if(backupNeeded)
					list.add(ft);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult dir(FileTree ft, AboutFile sourceAF, AboutFile backupAF) {
				return FileVisitResult.CONTINUE;
			}
		});
		config.setBackupFiles(list);
		runLater(() -> updateConfigView(backupWalked));
	}
	private void updateConfigView(boolean backupWalked) {
		configView.setSourceSizeFileCount(sourceSize, sourceFileCount)
		.setSourceDirCount(sourceDirCount);
		
		if(backupWalked) {
			configView.setTargetSizeFileCount(targetSize, targetFileCount)
			.setTargetDirCount(targetDirCount);
		}
		if(!config.isDisabled())
			configView.updateFileTree();
	}

	private void walk(Path start, Predicate<Path> excluder, WalkType walkType) throws IOException {
		if(Files.notExists(start)) {
			if(walkType == WalkType.BACKUP) {
				runLater(() -> {
					configView.setTargetSizeFileCount(targetSize, targetFileCount);
					configView.setTargetDirCount(targetDirCount);
				});
			}
			return;
		}
		this.excluder = excluder;
		this.walkType = walkType;
		nameCount = start.getNameCount();
		Files.walkFileTree(start, this);
	}

	private void fileUpdate(long size) {
		if(!listWalk) {
			if(walkType == WalkType.BACKUP) {
				targetSize += size;
				targetFileCount++;
				runLater(() -> configView.setTargetSizeFileCount(targetSize, targetFileCount));
			} else {
				sourceSize += size;
				sourceFileCount++;

				runLater(() -> configView.setSourceSizeFileCount(sourceSize, sourceFileCount));
			}
		}
		else  {
			sourceFileCount++;
			runLater(() -> listView.setFileCount(sourceFileCount));
		}
	}

	private void dirUpdate() {
		if(!listWalk) {
			if(walkType == WalkType.BACKUP) {
				targetDirCount++;
				runLater(() -> configView.setTargetDirCount(targetDirCount));
			} else {
				sourceDirCount++;
				runLater(() -> configView.setSourceDirCount(sourceDirCount));
			}
		} else {
			sourceDirCount++;
			runLater(() -> listView.setDirCount(sourceDirCount));
		}
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return FileVisitResult.TERMINATE;

		if(excluder.test(dir)) {
			excludeFilesList.add(dir);
			return FileVisitResult.SKIP_SUBTREE;
		}
		int end = dir.getNameCount();
		if(end != nameCount) {
			dirUpdate();
			FileTree ft = rootTree.addDirectory(dir.subpath(nameCount, end), dir, listWalk ? null : new AboutFile(attrs), walkType);

			if(!listWalk && skipOptions.contains(SkipBackupOption.DIR_NOT_MODIFIED) && ft.getModifiedTime() == ft.getSourceAboutFile().modifiedTime)
				return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return FileVisitResult.TERMINATE;

		if(excluder.test(file)){
			excludeFilesList.add(file);
			return FileVisitResult.CONTINUE;
		}

		AboutFile af = listWalk ? null : new AboutFile(attrs);
		fileUpdate(listWalk ? 0 : af.getSize());
		
		rootTree.addFile(file.subpath(nameCount, file.getNameCount()), file, af, walkType);
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}


