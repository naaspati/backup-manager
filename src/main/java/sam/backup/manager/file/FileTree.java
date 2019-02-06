package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Path;

import sam.backup.manager.walk.WalkMode;

public interface FileTree extends Dir {
	Attr EMPTY_ATTR = new Attr(-1, -1);
	Attrs EMPTY_ATTRS = new Attrs(EMPTY_ATTR); 
	
	void save() throws IOException;
	void forcedMarkUpdated();
	void walkCompleted();
	void walkStarted(Path start);
	Dir addDirectory(Path dir, Attr attr, WalkMode walkMode);
	void setAttr(Attr attr, WalkMode walkMode, Path dir);
	FileEntity addFile(Path file, Attr af, WalkMode walkMode);
}
