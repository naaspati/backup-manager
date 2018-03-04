package sam.backup.manager.walk;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.AboutFile;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.file.FileTree;
import sam.myutils.fileutils.FilesUtils;

public 	class WalkTask implements Runnable, FileVisitor<Path> {
	private final ConfigView configView;
	private final ListingView listView;
	private final Config config;
	private volatile  long sourceSize, targetSize;
	private volatile int sourceFileCount, sourceDirCount, targetFileCount, targetDirCount;
	private final boolean listWalk;
	private final ICanceler canceler;
	private final List<Path> excludeFilesList = new ArrayList<>();
	private final FileTree rootTree;
	private WalkType walkType;
	private boolean skipDirNotModified;
	private boolean skipModifiedCheck;
	private boolean skipFiles;

	private Predicate<Path> excluder, includer;
	private int nameCount;

	private  WalkTask(ConfigView configView, ListingView listView, WalkType walkType) {
		config = configView != null ? configView.getConfig() : listView.getConfig();
		this.rootTree = config.getFileTree();
		
		Set<WalkSkip> skips = config.getWalkSkips();
		this.skipDirNotModified = skips.contains(WalkSkip.DIR_NOT_MODIFIED);
		this.skipFiles = skips.contains(WalkSkip.FILES);
		
		this.skipModifiedCheck = config.isNoModifiedCheck();
		this.walkType = walkType;
		this.configView = configView;
		this.listView = listView;
		this.canceler = configView == null ? listView : configView;
		this.listWalk = listView != null;
	}
	public WalkTask(ConfigView configView, WalkType walkType) {
		this(configView, null, walkType);
	}
	public WalkTask(ListingView lv) {
		this(null, lv, WalkType.LIST);
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
				walk(root, config.getSourceExcluder(), config.getSourceIncluder(), walkType);
			if(listWalk) {
				listView.updateRootFileTree();
				return;
			}
			config.setSourceWalkCompleted(true);
			sourceWalkFailed = false;

			if(config.getTarget() != null && !config.isNoBackupWalk()) 
				walk(config.getTarget(), config.getTargetExcluder(), p -> false, WalkType.BACKUP);
		} catch (IOException e) {
			String s = sourceWalkFailed ? "Source walk failed: "+config.getSource() : "Target walk failed: "+config.getTarget();
			runLater(() -> configView.setError(s, e));
			return;
		}

		rootTree.walkCompleted(config);
		saveExcludeFilesList();
		updateBackups();
	}
	private void saveExcludeFilesList() {
		if(excludeFilesList.isEmpty())
			return;
		StringBuilder sb = new StringBuilder();
		Path root = config.getSource();
		int count = root.getNameCount();
		sb.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))).append('\n');
		sb.append(root).append('\n');

		for (Path path : excludeFilesList) {
			if(path.startsWith(root))
				sb.append("   ").append(path.subpath(count, path.getNameCount())).append('\n');
			else
				sb.append(path).append('\n');
		}
		sb.append("\n\n-------------------------------------------------\n\n");

		try {
			Path p = Utils.APP_DATA.resolve("excluded-files/"+(listWalk ? "list/" : "config/")+config.getSource().hashCode()+".txt");
			Files.createDirectories(p.getParent());
			FilesUtils.appendFileAtTop(sb.toString().getBytes(), p);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void updateBackups() {
		boolean backupWalked = !(config.isNoBackupWalk() || !RootConfig.backupDriveFound());
		boolean deleteBackupIfSourceNotExists = backupWalked && config.istDeleteBackupIfSourceNotExists(); 

		List<FileEntity> backupList = new ArrayList<>();
		List<FileEntity> delete = new ArrayList<>();

		rootTree.walk(new FileTreeWalker() {
			@Override
			public FileVisitResult file(FileEntity ft, AboutFile source, AboutFile backup) {
				boolean backupNeeded = false;

				String reason = null;
				
				if(deleteBackupIfSourceNotExists && source == null && backup != null) {
					delete.add(ft);
					ft.setDeleteBackup(true);
					return FileVisitResult.CONTINUE;
				}
				if(source != null) {
					if(backupWalked && backup == null) {
						reason = "File not found in backup";
						backupNeeded = true;
					}
					if(!backupNeeded && !backupWalked && ft.isNew()){
						reason = "New File";
						backupNeeded = true;
					}

					// if !onlyExistsCheck then check modified time;
					backupNeeded = backupNeeded || (!skipModifiedCheck && source.modifiedTime != ft.getModifiedTime());
					if(backupNeeded) reason = reason != null ? reason : "File Modified";
				}
				ft.setBackupNeeded(backupNeeded, reason);
				if(backupNeeded)
					backupList.add(ft);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult dir(DirEntity ft, AboutFile sourceAF, AboutFile backupAF) {
				return FileVisitResult.CONTINUE;
			}
		});
		
		config.setBackupFiles(backupList);
		config.setDeleteBackupFilesList(delete);
		
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
			configView.updateRootFileTree();
	}

	private void walk(Path start, Predicate<Path> excluder,Predicate<Path> includer, WalkType walkType) throws IOException {
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
		this.includer = includer;
		this.walkType = walkType;
		nameCount = start.getNameCount();
		Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), config.getDepth(), this);
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
	//TODO  preVisitDirectory
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return FileVisitResult.TERMINATE;

		int end = dir.getNameCount();

		if(end != nameCount && include(dir)) {
			dirUpdate();
			DirEntity ft = rootTree.addDirectory(dir.subpath(nameCount, dir.getNameCount()), dir, attrs.lastModifiedTime().toMillis(), walkType);

			if(skipDirNotModified && ft.getModifiedTime() == ft.getSourceAboutFile().modifiedTime)
				return FileVisitResult.SKIP_SUBTREE;
		}
		else if(end != nameCount) {
			excludeFilesList.add(dir);
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}

	// TODO visitFile
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return FileVisitResult.TERMINATE;
		if(skipFiles)
			return FileVisitResult.CONTINUE;
		
		if(include(file)) {
			AboutFile af = new AboutFile(attrs);
			fileUpdate(af.getSize());
			rootTree.addFile(file.subpath(nameCount, file.getNameCount()),file, af, walkType);
		}
		else 
			excludeFilesList.add(file);
		
		return FileVisitResult.CONTINUE;
	}

	private boolean include(Path file) {
		return includer.test(file) || !excluder.test(file);
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


