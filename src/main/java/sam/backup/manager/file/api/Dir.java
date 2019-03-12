package sam.backup.manager.file.api;

import java.util.function.Predicate;

public interface  Dir extends FileEntity, Iterable<FileEntity> {
	int childrenCount();
	default boolean isEmpty() {
		return childrenCount() == 0;
	}
	public void walk(FileTreeWalker walker);
	FilteredDir filtered(Predicate<FileEntity> filter);
	int countFilesInTree();
}

