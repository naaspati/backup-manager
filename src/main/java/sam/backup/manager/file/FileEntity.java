package sam.backup.manager.file;

import java.nio.file.Path;

import sam.backup.manager.file.FileTreeReader.Values;

public class FileEntity extends FileTreeEntity {
	FileEntity(Path path, DirEntity parent) {
		super(path, parent);
	}
	FileEntity(Values v, DirEntity parent) {
		super(v, parent);

		if(v.isDirectory())
			throw new IllegalArgumentException("not a file: "+v);
	}
	@Override
	public boolean isDirectory() {
		return false;
	}

	private boolean backup, delete, copied;
	private String reason;

	@Override
	public boolean isBackupNeeded() {
		return backup;
	}
	public void setBackupNeeded(boolean needed, String reason) {
		this.backup = needed;
		this.reason = reason;
		if(getParent() != null)
			getParent().backupNeeded(this, needed);
	}
	public String getReason() {
		return reason;
	}
	@Override
	public boolean isDeleteFromBackup() {
		return delete;
	}
	public void setDeletedFromBackup() {
		if(delete)
			throw new IllegalStateException("second time access");

		this.delete = true;
		if(getParent() != null)
			getParent().deleted(this);
	}
	@Override
	public boolean isCopied() {
		return copied;
	}
	public void setCopied() {
		if(copied)
			throw new IllegalStateException("second time access");

		this.copied = true;
		setUpdated();
		if(getParent() != null)
			getParent().copied(this);
	}

}
