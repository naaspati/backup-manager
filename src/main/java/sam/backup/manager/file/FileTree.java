package sam.backup.manager.file;

import static sam.backup.manager.extra.WalkType.BACKUP;
import static sam.backup.manager.extra.WalkType.NEW_SOURCE;
import static sam.backup.manager.extra.WalkType.SOURCE;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.WalkType;

public class FileTree implements Serializable {
	private static final long serialVersionUID = -3216725012618093594L;

	private final String pathString;
	private List<FileTree> children;
	private long modifiedTime; 
	private final boolean isDirectory;

	private transient boolean copied;
	private transient Path path;
	private transient Path fullPath;
	private transient Path target;
	private transient AboutFile sourceAF, backupAF;

	public FileTree(Path path, boolean isDirectory) {
		this.pathString = path.toString();
		this.path = path;
		this.isDirectory = isDirectory;
	}
	public List<FileTree> getChildren() {
		return children;
	}
	public Path getPath() {
		return path = path != null ? path : Paths.get(pathString);
	}
	public Path getSourcePath() {
		return fullPath;
	}
	public boolean backupNeeded(boolean onlyExistsCheck) {
		if(isDirectory)
			return true;
		if(sourceAF == null)
			return false;
		if(onlyExistsCheck)
			return backupAF == null;
		
		return backupAF == null || modifiedTime != sourceAF.getModifiedTime().toMillis()  || sourceAF.getSize() != backupAF.getSize();
	}
	public boolean isCopied() {
		return copied || (backupAF != null && sourceAF != null && modifiedTime == sourceAF.getModifiedTime().toMillis());
	}
	public void setCopied() {
		copied = true;
		modifiedTime = sourceAF.getModifiedTime().toMillis();
	}
	public boolean isNew() {
		return backupAF == null;
	}
	public boolean isDirectory() {
		return isDirectory;
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
					if(ft.getPath().equals(partialPath)) {
						ft.setAboutFile(aboutFile, walkType, fullpath);
						return ft;
					}
				}
			}
			FileTree ft = new FileTree(partialPath, isDirectory);
			if(children == null) children = new ArrayList<>();
			children.add(ft);
			ft.setAboutFile(aboutFile, walkType, fullpath);
			return ft;
		}
		else {
			Path p2 = partialPath.getName(0);
			for (FileTree ft : children) {
				if(p2.equals(ft.getPath())) {
					ft.add(partialPath.subpath(1, partialPath.getNameCount()),fullpath, aboutFile,walkType, isDirectory);
					return ft;
				} 
			}
		}
		throw new IllegalStateException("no parent found for name: "+partialPath+"  fullpath: "+fullpath);
	}
	public void setAboutFile(AboutFile aboutFile, WalkType walkType, Path fullpath) {
		if(walkType == SOURCE || walkType == NEW_SOURCE) {
			sourceAF = aboutFile ;
			this.fullPath = fullpath;
		}
		else if(walkType == BACKUP) 
			backupAF = aboutFile;
	}
	public void append(StringBuilder sb) {
		append(new char[0], sb);
	}
	private void append(final char[] separator, final StringBuilder sb) {
		for (FileTree f : children) {
			sb.append(separator).append(f.pathString);
			if(modifiedTime != 0)
				sb.append('>').append(modifiedTime);
			sb.append('\n');

			if(f.isDirectory) {
				int length = separator.length;
				char[] separator2 = Arrays.copyOf(separator, length + f.pathString.length());
				Arrays.fill(separator2, length, separator2.length, ' ');
				separator2[separator2.length - 1] = '|';
				f.append(separator2, sb);
			}
		}
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
		this.target = p.resolve(getPath());
		_calculateTargetPath(this.target); 
	}
	private void _calculateTargetPath(Path p) {
		if(children == null || children.isEmpty())
			return;

		for (FileTree f : children) 
			f.calculateTargetPath(p); 
	}
}

