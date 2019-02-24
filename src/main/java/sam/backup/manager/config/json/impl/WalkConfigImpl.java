package sam.backup.manager.config.json.impl;

import static sam.backup.manager.config.api.ConfigManager.either;

import sam.backup.manager.config.api.WalkConfig;
import sam.backup.manager.config.json.impl.Settable;

class WalkConfigImpl implements Settable,WalkConfig {
	private Integer depth;
	private Boolean walkBackup;
	private Boolean skipDirNotModified;
	private Boolean skipFiles;

	private final WalkConfigImpl global;

	WalkConfigImpl(WalkConfigImpl global) {
		this.global = global;
	}

	@Override
	public void set(String key, Object value) {
		switch (key) {
			case "depth": 
				depth = (Integer)value;
				break;
			case "walkBackup": 
				walkBackup = (Boolean)value;
				break;
			case "skipDirNotModified": 
				skipDirNotModified = (Boolean)value;
				break;
			case "skipFiles": 
				skipFiles = (Boolean)value;
				break;
			default:
				throw new IllegalArgumentException("unknown key: "+key);
		}
	}

	@Override
	public boolean walkBackup() {
		return either(walkBackup, global.walkBackup, false);
	}
	@Override
	public boolean skipDirNotModified() {
		return either(skipDirNotModified, global.skipDirNotModified, false);
	}
	@Override
	public boolean skipFiles() {
		return either(skipFiles, global.skipFiles, false);
	}
	@Override
	public int getDepth() {
		return depth == null ? Integer.MAX_VALUE : depth;
	}
}
