package sam.backup.manager.file.api;

import sam.backup.manager.config.impl.PathWrap;

public interface FileEntity {
	Dir getParent();
	Attrs getAttrs(Type type);
	boolean isDirectory();
	Status getStatus();
	String getName();
	PathWrap getPath(Type type);
	long getSourceSize();
}
