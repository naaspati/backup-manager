package sam.backup.manager.file.api;

import java.io.IOException;
import java.nio.file.Path;

public interface FileTree extends Dir {
	Attr EMPTY_ATTR = new Attr(-1, -1);
	Attrs EMPTY_ATTRS = new Attrs(EMPTY_ATTR); 
	
	void save() throws IOException;
	void forcedMarkUpdated();
	FileTreeEditor getEditor(Path start);
	boolean isWalked(Dir ft);
}
