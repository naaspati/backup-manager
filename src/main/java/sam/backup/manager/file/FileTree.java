package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.walk.WalkMode;

public class FileTree extends DirEntity {
	private final Config config;
	public FileTree(Path path) {
		super(path, null);
		this.config = null;
	}
	public FileTree(Config config) {
		super(config.getSource(), null);
		this.config = config;
	}
	FileTree(Config config, String fileNameString, Attrs sourceAttr, Attrs backupAttr) {
		super(fileNameString, null, sourceAttr, backupAttr);
		this.config = config;
	}
	public Config getConfig() {
		return config;
	}
	
	private boolean walkStarted;
	private int nameCount;
	private Path rootPath;
	public void walkStarted(Path root) {
		Objects.requireNonNull(root);
		walkStarted = true;
		rootPath = root;
		nameCount = root.getNameCount();
		setWalked(true);
	}

	private volatile DirLocaltor locator;
	private volatile Path currentParent;
	private volatile DirLocaltor currentLocator;
	
	private final class DirLocaltor {
		private final DirEntity value;
		private final Map<Path, DirLocaltor> map = new HashMap<>();

		DirLocaltor(DirEntity value) {
			this.value = value;
		}
		FileTreeEntity walk(Path subpath, boolean isDir) throws IOException {
			if(subpath.getNameCount() == 1)
				return add(subpath, isDir);
			else if(subpath.getParent().equals(currentParent))
				return currentLocator.add(subpath.getFileName(), isDir);
			else 
				return walk2(subpath, 0, isDir);
		}
		FileTreeEntity walk2(Path subpath, int index, boolean isDir) throws IOException {
			Path name = subpath.getName(index);

			if(subpath.getNameCount() == index + 1) {
				setCurrent(subpath);
				return add(name, isDir);
			}

			DirLocaltor dl = map.get(name);
			if(dl == null) {
				FileTreeEntity e = add(name, isDir);
				if(!e.isDirectory()) {
					setCurrent(subpath);
					return e;
				}
				dl = map.get(name);
			}
			if(dl == null) 
				throw new IOException(Utils.format("failed to complete walk: subpath:%s, index:%s, isDir:%s ", subpath, index, isDir));

			return dl.walk2(subpath, index+1, isDir);
		}
		private FileTreeEntity add(Path name, boolean isDir) throws IOException {
			FileTreeEntity entity = value.addChild(name, isDir);

			if(entity.isDirectory())
				map.put(name, new DirLocaltor(entity.asDir()));

			return entity;
		}
		public void setCurrent(Path subpath) {
			currentLocator = this;
			currentParent = subpath.getParent();
		}
	}
	public int getNameCount() {
		return nameCount;
	}
	public boolean isRootPath(Path dir) {
		return dir.equals(rootPath);
	}
	public DirEntity addDirectory(Path fullpath, Attrs af, WalkMode walkType) throws IOException {
		return add(fullpath, af, walkType, true).asDir();
	}
	public FileEntity addFile(Path fullpath, Attrs af, WalkMode walkType) throws IOException {
		return add(fullpath, af, walkType, false).asFile();
	}
	private FileTreeEntity add(Path fullpath, Attrs af, WalkMode walkType, boolean isDirectory) throws IOException {
		if(!walkStarted)
			throw new IllegalStateException("walk not started");

		if(isDirectory && isRootPath(fullpath))
			throw new IOException("invalied dir: root cannot be added to FileTree: "+fullpath);

		if(!fullpath.startsWith(rootPath))
			throw new IOException("invalied file: "+fullpath);

		if(locator == null) {
			locator = new DirLocaltor(this);
			currentLocator = locator;
		}
		FileTreeEntity e = locator.walk(subpath(fullpath), isDirectory);
		e.setAttrs(af, walkType, fullpath);
		return e;
	}
	private Path subpath(Path file) {
		return file.subpath(nameCount, file.getNameCount());
	}
	
	public void walkCompleted() {
		walkStarted = false;
		nameCount = -100;
		rootPath = null;
		locator = null;
		currentParent = null;
		currentLocator = null;
	}
	@Override
	public Path getSourcePath() {
		return getFileName();
	}
	@Override
	public Path getBackupPath() {
		return config == null ? null : config.getTarget();
	}
	@Override
	public AttrsKeeper getSourceAttrs() {
		if(super.getSourceAttrs().getPath() != getFileName())
			super.getSourceAttrs().setPath(getFileName());
		
		return super.getSourceAttrs();
	}
	@Override
	public AttrsKeeper getBackupAttrs() {
		if(config != null && super.getBackupAttrs().getPath() != config.getTarget())
			super.getBackupAttrs().setPath(config.getTarget());
		
		return super.getBackupAttrs();
	}
	public FilteredFileTree getFilteredView(Predicate<FileTreeEntity> filter) {
		return new FilteredFileTree(this, filter); 
	}

	public void setAttr(Attrs attr, WalkMode walkType, Path fullpath) {
		super.setAttrs(attr, walkType, fullpath);
	}
	public void forcedMarkUpdated() {
		walk(new SimpleFileTreeWalker() {
			@Override
			public FileVisitResult file(FileEntity ft) {
				ft.setCopied(true);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult dir(DirEntity ft) {
				ft.markUpdated();
				return FileVisitResult.CONTINUE;
			}
		});
		markUpdated();
	}
}
