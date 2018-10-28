package sam.backup.manager.file;

import java.util.Objects;

public class FileTreeEntityStatus {
	protected boolean backup, copied, backupDelete;
	protected String backupReason;
	
	public void setBackupDeletable(boolean backupDelete) {
		this.backupDelete = backupDelete;
	}
	public boolean isBackupDeletable() {
		return backupDelete;
	}
	public boolean isCopied() {
		return copied;
	}
	public boolean isBackupable() {
		return backup;
	}
	public String getBackupReason() {
		return backupReason;
	}
	public void setCopied(boolean b) {
		copied = b;
	}
	public void setBackupable(boolean b) {
		backup = b;
	}
	public void setBackupable(boolean b, String reason) {
		backup = b;
		this.backupReason = Objects.requireNonNull(reason);
		
		/*
		 * if(b) {
			FileTreeEntity ft = (FileTreeEntity) this;
			System.out.println(ft.getfileNameString()+" - - "+ft.getSourceAttrs().getCurrent());
		}
		 */
	}


}
