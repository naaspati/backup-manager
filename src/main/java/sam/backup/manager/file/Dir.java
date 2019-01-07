package sam.backup.manager.file;

public interface  Dir extends FileEntity, Iterable<FileEntity> {
	int childrenCount();
	FileEntity addFile(String filename);
	Dir addDir(String filename);
}

