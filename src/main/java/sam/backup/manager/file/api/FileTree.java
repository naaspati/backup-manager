package sam.backup.manager.file.api;

import java.nio.file.Path;

import sam.backup.manager.file.FileTreeDeleter;

public interface FileTree extends Dir {
	static final int EMPTY_ATTR_MARKER = -1;
	static final int DELETED_ATTR_MARKER = -10;
	
	Attr EMPTY_ATTR = new Attr(EMPTY_ATTR_MARKER, EMPTY_ATTR_MARKER);
	Attr DELETED_ATTR = new Attr(DELETED_ATTR_MARKER, DELETED_ATTR_MARKER);
	Attrs EMPTY_ATTRS = new Attrs(EMPTY_ATTR); 
	
	FileTreeDeleter getDeleter();
	FileTreeEditor getEditor(Path start);
	boolean isWalked(Dir ft);
}
