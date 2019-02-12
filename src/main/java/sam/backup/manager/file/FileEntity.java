package sam.backup.manager.file;

import sam.backup.manager.config.PathWrap;

public interface FileEntity {
	int getId();
	Dir getParent();
	Attrs getBackupAttrs();
	Attrs getSourceAttrs();
	boolean isDirectory();
	Status getStatus();
	String getName();
	PathWrap getSourcePath();
	PathWrap getBackupPath();
	boolean delete();
	long getSourceSize();
}
