package sam.backup.manager.file;

import static sam.backup.manager.walk.WalkMode.BACKUP;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

import sam.backup.manager.extra.Utils;
import sam.backup.manager.walk.WalkMode;

public abstract class FileTreeEntity extends FileTreeEntityStatus {
	private String fileNameString;
	protected static final Logger LOGGER =  Utils.getLogger(FileTreeEntity.class);

	private Path fileName; // lazy initiated  
	private final DirEntity parent;

	private final AttrsKeeper sourceAttrs;
	private final AttrsKeeper backupAttrs;

	/**
	 * to create clone
	 * @param f
	 */
	FileTreeEntity(FileTreeEntity f, DirEntity parent) {
		this.fileNameString = f.fileNameString;
		this.fileName = f.fileName;
		this.parent = parent;
		this.sourceAttrs = f.sourceAttrs;
		this.backupAttrs = f.backupAttrs;
		this.backup = f.backup;
		this.copied = f.copied;
		this.backupReason = f.backupReason;
	}

	FileTreeEntity(Path path, DirEntity parent) {
		this.fileName = path;
		this.sourceAttrs = new AttrsKeeper();
		this.backupAttrs = new AttrsKeeper();
		this.parent = parent;
	}

	public FileTreeEntity(String fileNameString, DirEntity parent, Attrs sourceAttr, Attrs backupAttr) {
		this.fileNameString = fileNameString;
		this.parent = parent;
		this.sourceAttrs = new AttrsKeeper(sourceAttr);
		this.backupAttrs = new AttrsKeeper(backupAttr);
	}

	public abstract boolean isDirectory();

	public DirEntity getParent() {
		return parent;
	}
	public DirEntity asDir() {
		return (DirEntity)this;
	}
	public FileEntity asFile() {
		return (FileEntity)this;
	}
	public Path getFileName() {
		if(fileName != null)
			return fileName;
		return fileName = Paths.get(fileNameString); 
	}
	public String getfileNameString() {
		if(fileNameString == null)
			fileNameString = fileName.toString();

		return fileNameString;
	}
	public AttrsKeeper getBackupAttrs() {
		return backupAttrs;
	}
	public AttrsKeeper getSourceAttrs() {
		return sourceAttrs;
	}
	void setAttrs(Attrs attr, WalkMode walkType, Path fullpath) {
		(walkType == BACKUP ? backupAttrs : sourceAttrs).set(attr, fullpath);
	}

	@Override
	public String toString() {
		return getfileNameString();
	}
	protected void setUpdated() {
		getBackupAttrs().setUpdated();
		getSourceAttrs().setUpdated();
	}
	public long getSourceSize() {
		return sourceAttrs.getSize();
	}
	public Path getSourcePath() {
		Path  p = sourceAttrs.getPath();
		if(p == null) {
			p = parent.getSourcePath();
			if(p != null)
				p = p.resolve(getFileName());
		}
		return p;
	}
	public Path getBackupPath() {
		Path  p = backupAttrs.getPath();
		if(p == null) {
			p = parent.getBackupPath();
			if(p != null)
				p = p.resolve(getFileName());
		}
		return p;
	}
	protected void markUpdated() {
		sourceAttrs.setUpdated();
		backupAttrs.setUpdated();
	}
	/**
	 * remove from parent
	 */
	public void remove() {
		if(parent != null)
			parent.remove(this);
	}
}