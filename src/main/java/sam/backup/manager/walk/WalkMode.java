package sam.backup.manager.walk;

public enum WalkMode {
	SOURCE, BACKUP, BOTH;

	boolean isSource() {
		return this == BOTH || this == SOURCE;
	}
	boolean isBackup() {
		return this == BOTH || this == BACKUP;
	}

}
