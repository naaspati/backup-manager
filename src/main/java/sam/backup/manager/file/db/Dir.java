package sam.backup.manager.file.db;

public interface  Dir extends FileEntity, Iterable<FileEntity> {
	int childrenCount();
	FileEntity addFile(String filename);
	Dir addDir(String filename);
}

