package sam.backup.manager.file;

import java.util.IdentityHashMap;
import java.util.function.Predicate;

import sam.backup.manager.config.PathWrap;


public class FilteredFileTree extends FilteredDir {

	FilteredFileTree(Dir dir, FilteredDir parent, Predicate<FileEntity> filter) {
		super(dir, parent, filter);
		// TODO Auto-generated constructor stub
	}

	public FilteredFileTree(IdentityHashMap<PathWrap, FileTree> fileTree, Predicate<FileEntity> filter) {
		
		// TODO Auto-generated constructor stub
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

}
