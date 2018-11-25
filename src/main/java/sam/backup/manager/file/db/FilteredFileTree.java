package sam.backup.manager.file.db;

import java.util.function.Predicate;


public class FilteredFileTree extends FilteredDir {

	FilteredFileTree(Dir dir, FilteredDir parent, Predicate<FileEntity> filter) {
		super(dir, parent, filter);
		// TODO Auto-generated constructor stub
	}

	public FilteredFileTree(FileTree fileTree, Predicate<FileEntity> filter) {
		// TODO Auto-generated constructor stub
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

}
