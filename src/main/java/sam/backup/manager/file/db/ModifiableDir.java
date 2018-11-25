package sam.backup.manager.file.db;

public interface ModifiableDir {
	int childrenCount();
	FileEntity addFile(String filename);
	Dir addDir(String filename);
}
