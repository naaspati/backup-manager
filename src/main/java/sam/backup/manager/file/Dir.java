package sam.backup.manager.file;

public interface  Dir extends FileEntity, Iterable<FileEntity> {
	int childrenCount();
	boolean isEmpty();
	FileEntity addFile(String filename);
	Dir addDir(String dirname);
	void setWalked(boolean b);
	boolean isWalked();
}

