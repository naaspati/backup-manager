package sam.backup.manager.config;

import static sam.backup.manager.extra.Utils.either;

public class WalkConfig {
	private Integer depth;
	private Boolean walkBackup;
	private Boolean skipDirNotModified;
	private Boolean skipFiles;
	private Boolean saveExcludeList;

	private transient WalkConfig rootConfig;

	void setRootConfig(WalkConfig rootConfig) {
		this.rootConfig = rootConfig;
	}
	public boolean walkBackup() {
		return either(walkBackup, rootConfig.walkBackup, false);
	}
	public boolean skipDirNotModified() {
		return either(skipDirNotModified, rootConfig.skipDirNotModified, false);
	}
	public boolean skipFiles() {
		return either(skipFiles, rootConfig.skipFiles, false);
	}
	public int getDepth() {
		return depth == null ? Integer.MAX_VALUE : depth;
	}
	public boolean saveExcludeList() {
		return either(saveExcludeList, rootConfig.saveExcludeList, false);
	}

}
