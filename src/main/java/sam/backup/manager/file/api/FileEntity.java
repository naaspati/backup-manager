package sam.backup.manager.file.api;

import sam.backup.manager.config.PathWrap;
import sam.backup.manager.file.Status;

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
	long getSourceSize();
}
