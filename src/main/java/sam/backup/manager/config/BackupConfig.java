package sam.backup.manager.config;

import static sam.backup.manager.extra.Utils.either;

public class BackupConfig {
	private Boolean checkModified, hardSync;
	
	private transient BackupConfig rootConfig;
	
	void setRootConfig(BackupConfig rootConfig) {
		this.rootConfig = rootConfig;
	}
	
	BackupConfig() {}
	
	public boolean checkModified() {
		return either(checkModified, rootConfig.checkModified, true);
	}

	public boolean hardSync() {
		return either(hardSync, rootConfig.hardSync, false);
	}
}
