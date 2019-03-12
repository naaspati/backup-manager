package sam.backup.manager.file;

import sam.backup.manager.file.api.Status;

interface Status2 extends Status {
	public static final int BACKUPABLE = 0;
	public static final int COPIED = 1;
	public static final int BACKUP_DELETABLE = 2;
	
	public static final int SIZE = 3;
	
	void set(int type, boolean value);
	boolean get(int type);
	void setBackupReason(String reason);

	@Override
	default void setBackupDeletable(boolean b) {
		set(BACKUP_DELETABLE, b);
	}
	@Override
	default boolean isBackupDeletable() {
		return get(BACKUP_DELETABLE);
	}
	@Override
	default boolean isCopied() {
		return get(COPIED);
	}
	@Override
	default boolean isBackupable() {
		return get(BACKUPABLE);
	}

	@Override
	default void setCopied(boolean b) {
		set(COPIED, b);
	}

	@Override
	default void setBackupable(boolean b) {
		set(BACKUPABLE, b);
	}
	@Override
	default void setBackupable(boolean b, String reason) {
		set(BACKUPABLE, b);
		setBackupReason(reason);
	}
}
