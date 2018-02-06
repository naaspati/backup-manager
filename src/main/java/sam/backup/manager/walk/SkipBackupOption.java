package sam.backup.manager.walk;

public enum SkipBackupOption {
	/**
	 * no backup needed if file exists in both source and backup (doesn't check modified time)
	 */
	FILE_EXISTS,
	/**
	 * if folder modified time for src and backup matches then folder subtree is skipped from being scanned
	 */
	DIR_NOT_MODIFIED,
}
