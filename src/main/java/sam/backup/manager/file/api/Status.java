package sam.backup.manager.file.api;

public interface Status {
	public void setBackupDeletable(boolean backupDelete);
	public boolean isBackupDeletable();
	public boolean isCopied();
	public boolean isBackupable();
	public String getBackupReason();
	public void setCopied(boolean b);
	public void setBackupable(boolean b);
	public void setBackupable(boolean b, String reason);
}
