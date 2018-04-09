package sam.backup.manager.extra;

public enum Options {
	/**
	 * if folder modified time for src and backup matches then folder subtree is skipped from being scanned
	 */
	SKIP_DIR_NOT_MODIFIED,
	/**
	 * dont list files (only dirs)
	 */
	SKIP_FILES

}
