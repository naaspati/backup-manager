package sam.backup.manager.file;

import sam.backup.manager.view.config.Deleter;
import sam.nopkg.Junk;


public class FilteredFileTree extends FilteredDir {

	FilteredFileTree(Dir dir, FilteredDir parent, FileEntityFilter filter) {
		super(dir, parent, filter);
		// TODO Auto-generated constructor stub
	}

	public FilteredFileTree(FileTree fileTree, FileEntityFilter filter) {
		// super(dir, parent, filter);
		super(null, null, filter);
		Junk .notYetImplemented();
		// FIXME Auto-generated constructor stub
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public void walk(Deleter d) {
		Junk.notYetImplemented();
		// this is temp solution to hide error in  Deleter
		// 
		// FIXME Auto-generated method stub
		
	}
}
