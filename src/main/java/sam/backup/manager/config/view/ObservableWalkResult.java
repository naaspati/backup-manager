package sam.backup.manager.config.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.walk.WalkResult;

public class ObservableWalkResult {
	private final ObservableSet<FileEntity> backups;
	private final ObservableSet<FileEntity> deletes;
	
	public ObservableWalkResult(WalkResult result) {
		this.backups = FXCollections.observableSet();
		backups.addAll(result.getBackups());
		
		this.deletes = FXCollections.observableSet();
		deletes.addAll(result.getDeletes());
	}
	public ObservableSet<FileEntity> getBackups() {
		return backups;
	}
	public ObservableSet<FileEntity> getDeletes() {
		return deletes;
	}
}
