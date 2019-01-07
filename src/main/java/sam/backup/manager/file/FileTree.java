package sam.backup.manager.file;

import java.util.Iterator;

public interface FileTree extends Dir {
	void save();
	void forcedMarkUpdated();
	int childrenCount(Dir ofDir);
	Iterator<FileEntity> iterator(Dir ofDir);
	FileEntity addFile(Dir parent, String filename);
	Dir addDir(Dir parent, String filename);
}
