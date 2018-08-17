package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import sam.backup.manager.config.BackupConfig;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeEntity;

class Walker implements FileVisitor<Path>{
	private static final Logger LOGGER = LoggerFactory.getLogger(Walker.class);
	
	
	private final boolean skipDirNotModified;
	private final boolean skipFiles;
	private final FileTree rootTree;
	private final ICanceler canceler;
	private final Config config;
	private final WalkListener listener;
	
	private WalkMode walkMode;
	private IFilter excluder;
	
	final List<Path> excludeFilesList = new ArrayList<>();
	

	public Walker(Config config, WalkListener listener, ICanceler canceler) {
		BackupConfig c = config.getBackupConfig();
		this.skipDirNotModified = c.skipDirNotModified();
		this.skipFiles = c.skipFiles();

		this.rootTree = config.getFileTree();
		this.canceler = canceler;
		this.config = config;
		this.listener = listener;
	}
	void walk(Path start, IFilter excluder, WalkMode mode) throws IOException {
		this.excluder = excluder;
		this.walkMode = mode;

		/*
		 * System.out.println("start: "+start);
	System.out.println("mode: "+mode);
	System.out.println("skipDirNotModified: "+skipDirNotModified);
	System.out.println("skipModifiedCheck: "+skipModifiedCheck);
	System.out.println("skipFiles: "+skipFiles);
		 */

		rootTree.walkStarted(start);
		Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), config.getBackupConfig().getDepth(), this);
	}


	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return TERMINATE;

		if(!checkPath(dir)) return SKIP_SUBTREE;

		if(rootTree.isRootPath(dir)) {
			rootTree.setAttr(new Attrs(attrs.lastModifiedTime().toMillis(), 0), walkMode, dir);
		} else if(include(dir)) {
			DirEntity ft = rootTree.addDirectory(dir, new Attrs(attrs.lastModifiedTime().toMillis(), 0), walkMode);
			listener.onDirFound(ft, walkMode);

			if(walkMode == WalkMode.BACKUP && !ft.isWalked()) {
				LOGGER.debug("backup walk skipped: {}", ft.getBackupPath());
				return SKIP_SUBTREE;
			}
			if(skipDirNotModified && !atrs(ft).isModified()) {
				LOGGER.debug("source walk skipped: {}", ft.getSourcePath());
				return SKIP_SUBTREE;
			}
			ft.setWalked(true);
		} else {
			excludeFilesList.add(dir);
			return SKIP_SUBTREE;
		}

		return CONTINUE;
	}
	private AttrsKeeper atrs(FileTreeEntity ft) {
		return walkMode == WalkMode.BACKUP ? ft.getBackupAttrs() : ft.getSourceAttrs();
	}
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return CONTINUE;
	}
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(canceler.isCancelled())
			return TERMINATE;

		if(skipFiles || !checkPath(file))
			return CONTINUE;

		if(include(file)) {
			Attrs af = new Attrs(attrs.lastModifiedTime().toMillis(), attrs.size());
			FileEntity ft  = rootTree.addFile(file, af, walkMode);
			listener.onFileFound(ft, af.getSize(), walkMode);
		} else 
			excludeFilesList.add(file);

		return CONTINUE;
	}
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return CONTINUE;
	}
	private boolean include(Path file) {
		return !excluder.test(file);
	}
	public static final int MAX_PATH_LENGTH = 260;

	private boolean checkPath(Path file) {
		String s = file.toString();

		if(s.length() > MAX_PATH_LENGTH) {
			LOGGER.error("max path length exceeded {} -> {}", s.length(), s);
			return false;
		}
		return true;
	}

}
