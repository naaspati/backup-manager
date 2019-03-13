package sam.backup.manager.file.api;

import sam.backup.manager.config.impl.PathWrap;

public interface FileEntity {
	Dir getParent();
	Attrs getBackupAttrs();
	Attrs getSourceAttrs();
	boolean isDirectory();
	Status getStatus();
	String getName();
	PathWrap getBackupPath();
	PathWrap getSourcePath();
	long getSourceSize();
}
