package sam.backup.manager.config;

import static sam.backup.manager.extra.Utils.either;

public class WalkConfig implements Settable {
	private Integer depth;
	private Boolean walkBackup;
	private Boolean skipDirNotModified;
	private Boolean skipFiles;
	private Boolean saveExcludeList;

	private final WalkConfig global;

	WalkConfig(WalkConfig global) {
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
			case "saveExcludeList": 
				saveExcludeList = (Boolean)value;
				break;
			default:
				throw new IllegalArgumentException("unknown key: "+key);
		}
	}

	public boolean walkBackup() {
		return either(walkBackup, global.walkBackup, false);
	}
	public boolean skipDirNotModified() {
		return either(skipDirNotModified, global.skipDirNotModified, false);
	}
	public boolean skipFiles() {
		return either(skipFiles, global.skipFiles, false);
	}
	public int getDepth() {
		return depth == null ? Integer.MAX_VALUE : depth;
	}
	public boolean saveExcludeList() {
		return either(saveExcludeList, global.saveExcludeList, false);
	}

}
