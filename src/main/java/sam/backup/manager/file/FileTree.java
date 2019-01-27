package sam.backup.manager.file;

import java.io.IOException;

public interface FileTree {
	Attr EMPTY_ATTR = new Attr(-1, -1);
	Attrs EMPTY_ATTRS = new Attrs(EMPTY_ATTR); 
	
	void save() throws IOException;
	void forcedMarkUpdated();
}
