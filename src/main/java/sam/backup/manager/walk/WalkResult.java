package sam.backup.manager.walk;

import java.util.List;

import sam.backup.manager.file.FileEntity;

public class WalkResult {
	private List<FileEntity> backupList;
	private List<FileEntity> delete;
	private final String failed;
	
	WalkResult(List<FileEntity> backupList, List<FileEntity> delete) {
		this.backupList = backupList;
		this.delete = delete;
		this.failed = null;
	}
	public WalkResult(String failedReason) {
		this.backupList = null;
		this.delete = null;
		this.failed = failedReason;
	}
	public String getFailedReason() {
		return failed;
	}
	public boolean isFailed() {
		return failed != null;
	}
	public List<FileEntity> getBackups() { return backupList; }
	public List<FileEntity> getDeletes() { return delete; }
}
