package sam.backup.manager.config;

import static sam.backup.manager.extra.Utils.either;

public class BackupConfig implements Settable {
	private Boolean checkModified, hardSync;
	private final BackupConfig global;
	
	public BackupConfig(BackupConfig global) {
		this.global = global;
	}
	
	@Override
	public void set(String key, Object value) {
		switch (key) {
			case "checkModified":
				checkModified = (Boolean)value;
				break;
			case "hardSync":
				hardSync = (Boolean)value;
				break;
			default:
				throw new IllegalArgumentException("unknown key: "+key);
		}
	}
	public boolean checkModified() {
		return either(checkModified, global.checkModified, true);
	}
	public boolean hardSync() {
		return either(hardSync, global.hardSync, false);
	}
}
