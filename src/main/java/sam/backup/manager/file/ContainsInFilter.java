package sam.backup.manager.file;

import java.util.Collection;

import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileEntityFilter;
import sam.collection.IntSet;

public class ContainsInFilter implements FileEntityFilter {
	private final IntSet files;
	private final IntSet dirs;

	public ContainsInFilter(Collection<? extends FileEntity> containsIn) {
		if(containsIn.isEmpty()) {
			this.files = null;
			this.dirs = null;
		} else {
			IntSet files = new IntSet();
			IntSet dirs = new IntSet();

			for (FileEntity f : containsIn) {
				if(f.isDirectory())
					dirs.add(f.getId());
				else
					files.add(f.getId());

				while((f = f.getParent()) != null) 
					dirs.add(f.getId());
			}

			this.files = files.isEmpty() ? null : files;
			this.dirs =  dirs.isEmpty()  ? null : dirs;	
		}
	}
	private static boolean check(IntSet set, FileEntity f) {
		if(set == null)
			return false;

		return set.contains(f.getId());
	}
	
	@Override
	public boolean test(FileEntity t) {
		if(t == null)
			return false;
		
		if(t.isDirectory())
			return check(dirs, t);
		else
			return check(files, t);			
	}

}
