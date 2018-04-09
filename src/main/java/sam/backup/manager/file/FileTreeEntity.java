package sam.backup.manager.file;

import static sam.backup.manager.walk.WalkMode.BACKUP;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.walk.WalkMode;

public abstract class FileTreeEntity {
	private String fileNameString;
	protected static final Logger LOGGER =  LogManager.getLogger(FileTreeEntity.class);

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
		this.delete = f.delete;
		this.reason = f.reason;
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

	private boolean backup, copied, delete;
	private String reason;

	public boolean isCopied() {
		return copied;
	}
	public boolean isBackupable() {
		return backup;
	}
	public String getBackupReason() {
		return reason;
	}
	public void setCopied(boolean b) {
		copied = b;
	}
	public void setBackupable(boolean b) {
		backup = b;
	}
	public void setBackupable(boolean b, String reason) {
		backup = b;
		this.reason = Objects.requireNonNull(reason);
	}
	public boolean isDeletable() {
		return delete;
	}
	public void setDeletable(boolean delete) {
		this.delete = delete;
	}
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
		return sourceAttrs.getPath();
	}
	public Path getBackupPath() {
		Path  p = backupAttrs.getPath();
		if(p == null)
			p = parent.getBackupPath().resolve(getFileName());
		return p;
	}
	protected void markUpdated() {
		sourceAttrs.setUpdated();
		backupAttrs.setUpdated();
	}
}