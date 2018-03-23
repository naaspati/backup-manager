package sam.backup.manager.file;

import static sam.backup.manager.walk.WalkMode.BACKUP;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import sam.backup.manager.file.FileTreeReader.Values;
import sam.backup.manager.walk.WalkMode;

public abstract class FileTreeEntity {
	private String fileNameString;

	private Path fileName; // lazy initiated  
	private final DirEntity parent;

	private final AttrsKeeper sourceAttrs;
	private final AttrsKeeper backupAttrs;

	FileTreeEntity(Path path, DirEntity parent) {
		this.fileName = path;
		this.sourceAttrs = new AttrsKeeper();
		this.backupAttrs = new AttrsKeeper();
		this.parent = parent;
	}
	FileTreeEntity(Values values, DirEntity parent) {
		this.fileNameString = values.getFilenameString();
		this.sourceAttrs = new AttrsKeeper(values.getSrcAttrs());
		this.backupAttrs = new AttrsKeeper(values.getBackupAttrs());
		this.parent = parent;
	}

	public abstract boolean isDirectory();
	
	private boolean backup, delete, copied;
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
	public boolean isDeletable() {
		return delete;
	}
	public void setCopied(boolean b) {
		copied = b;
	}
	public void setBackupable(boolean b, String reason) {
		backup = b;
		this.reason = Objects.requireNonNull(reason); 
	}
	public void setDeletable(boolean b) {
		delete = b;
	}
	public DirEntity getParent() {
		return parent;
	}
	DirEntity castDir() {
		return (DirEntity)this;
	}
	FileEntity castFile() {
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
}