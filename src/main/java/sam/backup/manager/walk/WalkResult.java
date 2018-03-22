package sam.backup.manager.walk;

import java.util.Collections;
import java.util.List;

import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;

public class WalkResult {
	private List<FileEntity> backupList;
	private List<FileTreeEntity> delete;
	private final String failed;
	
	WalkResult(List<FileEntity> backupList, List<FileTreeEntity> delete) {
		this.backupList = Collections.unmodifiableList(backupList);
		this.delete = Collections.unmodifiableList(delete);
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
	public List<FileTreeEntity> getDeletes() { return delete; }
}
