package sam.backup.manager.file.db;

import java.util.function.Consumer;

interface Serializer {
	FileImpl newFile(Dir parent, String filename);
	FileImpl newDir(Dir parent, String filename);
	void applyToAll(Consumer<FileImpl> action);
	FileTree getFileTree();
	void save();
	Attrs defaultAttrs();
}
