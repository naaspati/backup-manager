package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.IFilter;
import sam.backup.manager.config.api.WalkConfig;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeEditor;

class Walker implements FileVisitor<Path>, Callable<FileTree> {
	private static final Logger LOGGER = Utils.getLogger(Walker.class);
	
	private final boolean skipDirNotModified;
	private final boolean skipFiles;
	private final FileTree rootTree;
	private FileTreeEditor editor;
	private final Config config;
	private final WalkListener listener;
	private final Path start;
	private boolean isRoot = true;
	
	private WalkMode walkMode;
	private IFilter excluder;
	
	private final List<Path> excludeFilesList;

	public Walker(FileTree filetree, Config config, WalkListener listener, Path start, IFilter excluder, WalkMode mode, List<Path> exucludePaths) {
		WalkConfig c = config.getWalkConfig();
		this.skipDirNotModified = c.skipDirNotModified();
		this.skipFiles = c.skipFiles();
		this.start = start;
		this.excludeFilesList = exucludePaths;

		this.rootTree = filetree;
		this.config = config;
		this.listener = listener;
		this.excluder = excluder;
		this.walkMode = mode;
	}
	public FileTree call() throws IOException {
		try(FileTreeEditor editor = rootTree.getEditor(start)) {
			this.editor = editor;
			Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), config.getWalkConfig().getDepth(), this);
		}
		this.editor = null;
		return rootTree;
	}
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(isRoot) { 
			editor.setAttr(new Attr(attrs.lastModifiedTime().toMillis(), 0), walkMode, dir);
			isRoot = false;
		} else if(include(dir)) {
			Dir ft = editor.addDir(dir, new Attr(attrs.lastModifiedTime().toMillis(), 0), walkMode);
			listener.onDirFound(ft, walkMode);

			 if(walkMode == WalkMode.BACKUP && !editor.isWalked(ft)) {
				LOGGER.info(() -> "backup walk skipped: "+ ft.getBackupPath());
				return SKIP_SUBTREE;
			}
			
			if(skipDirNotModified && !atrs(ft).isModified()) {
				LOGGER.debug("source walk skipped: {}", ft.getSourcePath());
				return SKIP_SUBTREE;
			}
			editor.setWalked(ft, true);
		} else {
			excludeFilesList.add(dir);
			return SKIP_SUBTREE;
		}
		return CONTINUE;
	}
	private Attrs atrs(FileEntity ft) {
		return walkMode == WalkMode.BACKUP ? ft.getBackupAttrs() : ft.getSourceAttrs();
	}
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return CONTINUE;
	}
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(skipFiles)
			return CONTINUE;

		if(include(file)) {
			Attr af = new Attr(attrs.lastModifiedTime().toMillis(), attrs.size());
			FileEntity ft  = editor.addFile(file, af, walkMode);
			listener.onFileFound(ft, af.size(), walkMode);
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
}
