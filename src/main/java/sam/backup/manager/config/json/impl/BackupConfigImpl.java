package sam.backup.manager.config.json.impl;

import static sam.backup.manager.extra.Utils.either;

import sam.backup.manager.config.api.BackupConfig;

public class BackupConfigImpl implements Settable, BackupConfig {
	private Boolean checkModified, hardSync;
	private final BackupConfigImpl global;
	
	public BackupConfigImpl(BackupConfigImpl global) {
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
	@Override
	public boolean checkModified() {
		return either(checkModified, global.checkModified, true);
	}
	@Override
	public boolean hardSync() {
		return either(hardSync, global.hardSync, false);
	}
}
