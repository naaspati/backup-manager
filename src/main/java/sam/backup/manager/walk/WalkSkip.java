package sam.backup.manager.walk;

public enum WalkSkip {
	/**
	 * if folder modified time for src and backup matches then folder subtree is skipped from being scanned
	 */
	DIR_NOT_MODIFIED,
	/**
	 * dont list files (only dirs)
	 */
	FILES
}
