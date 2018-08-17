package sam.backup.manager.config;

public class BackupConfig {
	private Boolean walkBackup;
	private Boolean checkModified;
	private Integer depth;
	private Boolean skipDirNotModified;
	private Boolean skipFiles;
	
	private transient BackupConfig rootConfig;
	
	void setRootConfig(BackupConfig rootConfig) {
		this.rootConfig = rootConfig;
	}
	
	BackupConfig() {}
	
	public boolean walkBackup() {
		return either(walkBackup, rootConfig.walkBackup, false);
	}
	public boolean checkModified() {
		return either(checkModified, rootConfig.checkModified, true);
	}
	public boolean skipDirNotModified() {
		return either(skipDirNotModified, rootConfig.skipDirNotModified, false);
	}
	public boolean skipFiles() {
		return either(skipFiles, rootConfig.skipFiles, false);
	}
	protected <T> T either(T t1, T t2, T defaultValue) {
		if(t1 == null && t2 == null)
			return defaultValue;
		return t1 != null ? t1 : t2;
	}
	public int getDepth() {
		return depth == null ? Integer.MAX_VALUE : depth;
	}
}
