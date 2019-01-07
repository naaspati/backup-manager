package sam.backup.manager.file;

import java.util.Collection;
import java.util.function.Predicate;

import sam.collection.IntSet;

public class ContainsInFilter implements Predicate<FileEntity> {
	private IntSet files;
	private IntSet dirs;
	
	public ContainsInFilter(Collection<? extends FileEntity> containsIn) {
		if(containsIn.isEmpty()) {
			return;
		}
		
		dirs = new IntSet();
		files = new IntSet();
		
		for (FileEntity f : containsIn) {
			if(f.isDirectory())
				dirs.add(f.getId());
			else
				files.add(f.getId());
			
			while((f = f.getParent()) != null) dirs.add(f.getId());
		}
	}

	@Override
	public boolean test(FileEntity f) {
		if(files == null && dirs == null)
			return false;
		
		if(f.isDirectory())
			return dirs.contains(f.getId());
		else 
			return files.contains(f.getId());
	}
}
