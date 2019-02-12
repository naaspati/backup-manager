package sam.backup.manager.file;

public interface  Dir extends FileEntity, Iterable<FileEntity> {
	int childrenCount();
	boolean isEmpty();
	FileEntity addFile(String filename);
	Dir addDir(String dirname);
	public void walk(FileTreeWalker walker); //was removed
	int filesInTree(); // was removed
}

