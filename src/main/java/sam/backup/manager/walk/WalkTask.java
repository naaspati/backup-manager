package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.FileNotFoundException;
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.LoggerFactory;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.fx.popup.FxPopupShop;
import sam.myutils.fileutils.FilesUtils;

public class WalkTask implements Runnable, FileVisitor<Path> {
	private final Config config;
	private volatile  long sourceSize, targetSize;
	private volatile int sourceFileCount, sourceDirCount, targetFileCount, targetDirCount;
	private final ICanceler canceler;
	private final List<Path> excludeFilesList = new ArrayList<>();
	private final FileTree rootTree;
	private boolean skipDirNotModified;
	private boolean skipModifiedCheck;
	private boolean skipFiles;

	private final WalkMode fixedWalkMode;
	private WalkMode currentWalkMode;
	private final WalkListener listener;

	private Predicate<Path> excluder, includer;

	public WalkTask(Config config, WalkMode walkMode, ICanceler canceler, WalkListener listener) {
		this.config = config;
		this.rootTree = config.getFileTree();
		this.fixedWalkMode = walkMode;
		this.listener = listener;

		Set<WalkSkip> skips = config.getWalkSkips();
		this.skipDirNotModified = skips.contains(WalkSkip.DIR_NOT_MODIFIED);
		this.skipFiles = skips.contains(WalkSkip.FILES);

		this.skipModifiedCheck = config.isNoModifiedCheck();
		this.canceler = canceler;
	}

	private static final IdentityHashMap<FileTree, Void> sourceWalkCompleted = new IdentityHashMap<>();
	private static final IdentityHashMap<FileTree, Void> backupWalkCompleted = new IdentityHashMap<>();
	private boolean sourceWalked;
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
			if(fixedWalkMode.isSource()) {
				if(!sourceWalkCompleted.containsKey(rootTree)) {
					walk(root, config.getSourceExcluder(), config.getSourceIncluder(), WalkMode.SOURCE);
					sourceWalkCompleted.put(rootTree, null);
				}
				sourceWalked = true;
			}

			sourceWalkFailed = false;

			boolean walk = !(config.isNoBackupWalk() || !RootConfig.backupDriveFound());

			if(walk && fixedWalkMode.isBackup()){
				if(!backupWalkCompleted.containsKey(rootTree)) {
					walk(config.getTarget(), config.getTargetExcluder(), p -> false, WalkMode.BACKUP);
					backupWalkCompleted.put(rootTree, null);
				}
				backupWalked = true;
			}
		} catch (IOException e) {
			String s = sourceWalkFailed ? "Source walk failed: "+config.getSource() : "Target walk failed: "+config.getTarget();
			listener.walkFailed(s, e);
			return;
		}

		rootTree.walkCompleted(config.getTarget());
		saveExcludeFilesList();

		listener.walkCompleted(createResult());
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

		Path p = Utils.APP_DATA.resolve("excluded-files/"+(fixedWalkMode+"/")+config.getSource().hashCode()+".txt");
		try {
			Files.createDirectories(p.getParent());
			FilesUtils.appendFileAtTop(sb.toString().getBytes(), p);
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass()).error("error occured while saving: "+p, e);
			FxPopupShop.showHidePopup("error occured", 1500);
		}
	}
	private WalkResult createResult() {
		boolean deleteBackupIfSourceNotExists = backupWalked && config.istDeleteBackupIfSourceNotExists(); 

		List<FileEntity> backupList = new ArrayList<>();
		List<FileEntity> delete = new ArrayList<>();

		BackupNeeded backupNeeded = new BackupNeeded();

		rootTree.walk(new FileTreeWalker() {
			@Override
			public FileVisitResult file(FileEntity ft, AttrsKeeper sourceK, AttrsKeeper backupK) {
				Attrs source = sourceK.getCurrent();
				Attrs backup = backupK.getCurrent();

				if(deleteBackupIfSourceNotExists && source == null && backup != null) {
					delete.add(ft);
					return CONTINUE;
				}

				backupNeeded.clear();

				if(source != null) {
					backupNeeded
					.test(backupWalked && backup == null, "File not found in backup")
					.test(sourceK::isNew, "New File")
					.test(() -> !skipModifiedCheck && sourceK.isModified(), "File Modified");
				}

				ft.setBackupNeeded(backupNeeded.isNeeded(), backupNeeded.getReason());
				if(backupNeeded.isNeeded()) 
					backupList.add(ft);

				return CONTINUE;
			}
			@Override
			public FileVisitResult dir(DirEntity ft, AttrsKeeper s, AttrsKeeper b) {
				return CONTINUE;
			}
		});
		
		listener.update(new Update(sourceSize, sourceFileCount, sourceDirCount), !backupWalked ? null : new Update(targetSize, targetFileCount, targetDirCount));
		return new WalkResult(backupList, delete);
	}

	private void walk(Path start, Predicate<Path> excluder,Predicate<Path> includer, WalkMode mode) throws IOException {
		this.excluder = excluder;
		this.includer = includer;
		this.currentWalkMode = mode;

		rootTree.walkStarted(start);
		dirs.clear();
		Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), config.getDepth(), this);
	}

	private void dirUpdate() {
		update(0, false);
	}
	private void fileUpdate(long size) {
		update(size, true);
	}
	private void update(long size, boolean fileUpdate) {
		Update src = null, target = null; 

		if(currentWalkMode == WalkMode.SOURCE) {
			if(fileUpdate) {
				sourceSize += size;
				sourceFileCount++;	
			} else {
				sourceDirCount++;
			}

			src = new Update(sourceSize, sourceFileCount, sourceDirCount);
		} else if(currentWalkMode == WalkMode.BACKUP) {
			if(fileUpdate) {
				targetSize += size;
				targetFileCount++;
			} else {
				targetDirCount++;
			}
			target = new Update(targetSize, targetFileCount, targetDirCount);
		} else 
			throw new IllegalArgumentException("bad walkmode: "+currentWalkMode);

		listener.update(src, target);
	}
	private final HashMap<Path, DirEntity> dirs = new HashMap<>();

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return TERMINATE;

		boolean notRoot = !rootTree.isRootPath(dir); 

		if(notRoot && include(dir)) {
			dirUpdate();
			DirEntity ft = rootTree.addDirectory(dir, new Attrs(attrs.lastModifiedTime().toMillis(), 0), currentWalkMode);

			if(skipDirNotModified && !atrs(ft).isModified())
				return SKIP_SUBTREE;
			ft.setWalked(true);
			dirs.put(dir, ft);
		} else if(notRoot) {
			excludeFilesList.add(dir);
			return SKIP_SUBTREE;
		}
		return CONTINUE;
	}
	private AttrsKeeper atrs(FileTreeEntity ft) {
		return currentWalkMode == WalkMode.BACKUP ? ft.getBackupAttrs() : ft.getSourceAttrs();
	}
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if(!rootTree.isRootPath(dir))
			dirs.get(dir).computeSize(currentWalkMode);
		return CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return TERMINATE;
		if(skipFiles)
			return CONTINUE;

		if(include(file)) {
			Attrs af = new Attrs(attrs.lastModifiedTime().toMillis(), attrs.size());
			fileUpdate(af.getSize());
			rootTree.addFile(file, af, currentWalkMode);
		} else 
			excludeFilesList.add(file);

		return CONTINUE;
	}

	private boolean include(Path file) {
		return includer.test(file) || !excluder.test(file);
	}
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return CONTINUE;
	}
}


