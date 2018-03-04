package sam.backup.manager.file;

import java.nio.file.Path;

import sam.backup.manager.file.FileTreeReader.Values;

public class FileEntity extends FileTreeEntity {
	private boolean backupNeeded;
	private String backupReason;

	private boolean deleteBackup;
	private boolean copied;

	FileEntity(Path path) {
		super(path);
	}
	FileEntity(Values v) {
		super(v);
		if(v.isDirectory())
			throw new IllegalArgumentException("not a file: "+v);
	}
	@Override
	public boolean isBackupNeeded() {
		return backupNeeded;
	}
	public void setBackupNeeded(boolean backupNeeded, String reason) {
		this.backupNeeded = backupNeeded;
		this.backupReason = reason;
	}
	public boolean isDeleteBackup() {
		return deleteBackup;
	}
	public void setDeleteBackup(boolean deleteBackup) {
		this.deleteBackup = deleteBackup;
	}
	public String getBackupReason() {
		return backupReason;
	}
	@Override	
	public boolean isCopied() {
		return copied;
	}
	public void setCopied() {
		copied = true;
		if(getSourceAboutFile() != null)
			setModifiedTime(getSourceAboutFile().getModifiedTime());
	}
	@Override
	public boolean isDirectory() {
		return false;
	}
}
