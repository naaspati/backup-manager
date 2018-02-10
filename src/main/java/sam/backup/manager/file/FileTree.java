package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static sam.backup.manager.walk.WalkType.BACKUP;
import static sam.backup.manager.walk.WalkType.NEW_SOURCE;
import static sam.backup.manager.walk.WalkType.SOURCE;

import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sam.backup.manager.config.Config;
import sam.backup.manager.walk.WalkType;

public class FileTree implements Serializable {
	private static final long serialVersionUID = -3216725012618093594L;

	private final String pathString;
	private List<FileTree> children;
	private long modifiedTime = -1; 
	private final boolean isDirectory;

	private transient boolean copied;
	private transient boolean backupNeeded;
	private transient String backupReason;
	private transient Path path;
	private transient Path fullPath;
	private transient Path target;
	private transient AboutFile sourceAF, backupAF;

	public FileTree(Path fileName) {
		this(fileName, true);
	}
	private FileTree(Path path, boolean isDirectory) {
		this.pathString = path.toString();
		this.path = path;
		this.isDirectory = isDirectory;
	}
	public List<FileTree> getChildren() {
		return children;
	}
	public Path getFileName() {
		return path = path != null ? path : Paths.get(pathString);
	}
	public Path getSourcePath() {
		return fullPath;
	}
	public boolean isBackupNeeded() {
		if(isDirectory)
			return children != null && children.stream().anyMatch(FileTree::isBackupNeeded);

		return backupNeeded;
	}

	public void setBackupNeeded(boolean backupNeeded, String reason) {
		this.backupNeeded = backupNeeded;
		this.backupReason = reason;
	}
	public String getBackupReason() {
		return backupReason;
	}
	public boolean isCopied() {
		return copied;
	}
	public void setCopied() {
		copied = true;
		if(sourceAF != null)
			modifiedTime = sourceAF.getModifiedTime();
	}
	public boolean isDirectory() {
		return isDirectory;
	}
	public AboutFile getBackupAboutFile() {
		return backupAF;
	}
	public AboutFile getSourceAboutFile() {
		return sourceAF;
	}
	public FileTree addFile(Path partialPath, Path fullpath, AboutFile aboutFile, WalkType walkType) {
		return add(partialPath, fullpath, aboutFile, walkType, false);
	}
	public FileTree addDirectory(Path partialPath, Path fullpath, AboutFile aboutFile, WalkType walkType) {
		return add(partialPath, fullpath, aboutFile, walkType, true);
	}
	private FileTree add(Path partialPath, Path fullpath, AboutFile aboutFile, WalkType walkType, boolean isDirectory) {
		if(partialPath.getNameCount() == 1) {
			if(children != null && (walkType == SOURCE || walkType == BACKUP)) {
				for (FileTree ft : children) {
					if(ft.getFileName().equals(partialPath)) {
						ft.setAboutFile(aboutFile, walkType, fullpath);
						return ft;
					}
				}
			}
			FileTree ft = addChild(new FileTree(partialPath, isDirectory));
			ft.setAboutFile(aboutFile, walkType, fullpath);
			return ft;
		}
		else {
			Path p2 = partialPath.getName(0);
			for (FileTree ft : children) {
				if(p2.equals(ft.getFileName())) {
					ft.add(partialPath.subpath(1, partialPath.getNameCount()),fullpath, aboutFile,walkType, isDirectory);
					return ft;
				} 
			}
		}
		throw new IllegalStateException("no parent found for name: "+partialPath+"  fullpath: "+fullpath);
	}
	private FileTree addChild(FileTree child){
		if(children == null) children = new ArrayList<>();
		children.add(child);
		return child;
	}
	public void setAboutFile(AboutFile aboutFile, WalkType walkType, Path fullpath) {
		if(walkType == SOURCE || walkType == NEW_SOURCE) {
			sourceAF = aboutFile ;
			this.fullPath = fullpath;
		}
		else if(walkType == BACKUP) {
			backupAF = aboutFile;
		}
	}
	private void append(final char[] separator, final StringBuilder sb) {
		if(children == null || children.isEmpty()) return;

		for (FileTree f : children) {
			sb.append(separator).append(f.pathString).append('\n');

			if(f.isDirectory) {
				int length = separator.length;
				char[] sp2 = Arrays.copyOf(separator, length + 6);
				Arrays.fill(sp2, length, sp2.length, ' ');
				if(length != 2) {
					sp2[length - 1] = ' ';
					sp2[length - 2] = ' ';
				}
				sp2[sp2.length - 3] = '|';
				sp2[sp2.length - 2] = '_';
				sp2[sp2.length - 1] = '_';
				f.append(sp2, sb);
			}
		}
	}
	public String toTreeString() {
		StringBuilder sb = new StringBuilder().append(pathString).append('\n');
		append(new char[] {' ', '|'}, sb);
		return sb.toString();	
	}
	@Override
	public String toString() {
		return pathString;
	}
	public long getSourceSize() {
		return sourceAF.getSize();
	}
	public Path getTargetPath() {
		return target;
	}
	public void calculateTargetPath(Config config) {
		_calculateTargetPath(config.getTarget());
	}
	private void calculateTargetPath(Path p) {
		this.target = p.resolve(getFileName());
		_calculateTargetPath(this.target); 
	}
	private void _calculateTargetPath(Path p) {
		if(children == null || children.isEmpty())
			return;

		for (FileTree f : children) 
			f.calculateTargetPath(p); 
	}
	public long getModifiedTime() {
		return modifiedTime;
	}
	public void walk(FileTreeWalker walker) {
		_walk(walker);
	}
	private FileVisitResult _walk(FileTreeWalker walker) {
		for (FileTree f : children) {
			if(f.isDirectory && (f.children == null || f.children.isEmpty()))
				continue;

			FileVisitResult result = f.isDirectory ? walker.dir(f, f.sourceAF, f.backupAF) : walker.file(f, f.sourceAF, f.backupAF);

			if(result == TERMINATE)
				return TERMINATE;

			if(result == SKIP_SIBLINGS || result == SKIP_SUBTREE)
				return CONTINUE;

			if(f.isDirectory && f._walk(walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}
	public boolean isNew() {
		return modifiedTime == -1;
	}
}

