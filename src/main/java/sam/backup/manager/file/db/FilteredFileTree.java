package sam.backup.manager.file.db;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import sam.backup.manager.walk.WalkMode;


public class FilteredFileTree extends FilteredDir {
	private final FileTree ft;

	public FilteredFileTree(FileTree ft, WalkMode mode, Predicate<FileImpl> filter) {
		super(ft, null, filter);
		this.ft = ft;
		
		// FIXME
	//	ft.computeSize(mode);
	//	computeSize(mode);
	}
	public FilteredFileTree(FileTree fileTree, WalkMode source, Predicate<FileImpl> filter) {
		// TODO Auto-generated constructor stub
		
		//TODO
				List<FileImpl> files = new ArrayList<>();
				tree.getFiles()
				.forEach(f -> {
					if(f.getStatus().isBackupDeletable())
						files.add(f);
				});
				
				List<Dir> dirs = new ArrayList<>();
				tree.getDirs()
				.forEach(f -> {
					if(f.getStatus().isBackupDeletable())
						files.add(f);
				});
	}
	@Override
	public String getBackupPath() {
		return ft.getBackupPath();
	}
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
}
