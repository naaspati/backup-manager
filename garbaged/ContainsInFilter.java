package sam.backup.manager.file;

import java.util.Collection;

import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.Predicate<FileEntity>;
import sam.collection.IntSet;

import static sam.backup.manager.file.FileEntityWithId.*;

public class ContainsInFilter implements Predicate<FileEntity> {
	private final IntSet files;
	private final IntSet dirs;
	
	public static ContainsInFilter newInstance(Collection<? extends FileEntity> containsIn) {
		
	} 

	private ContainsInFilter(Collection<? extends FileEntity> containsIn) {
		if(containsIn.isEmpty()) {
			this.files = null;
			this.dirs = null;
		} else {
			IntSet files = new IntSet();
			IntSet dirs = new IntSet();

			for (FileEntity f : containsIn) {
				if(f.isDirectory())
					dirs.add(id(f));
				else
					files.add(id(f));

				while((f = f.getParent()) != null) 
					dirs.add(id(f));
			}

			this.files = files.isEmpty() ? null : files;
			this.dirs =  dirs.isEmpty()  ? null : dirs;	
		}
	}
	private static boolean check(IntSet set, FileEntity f) {
		if(set == null)
			return false;

		return set.contains(id(f));
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
