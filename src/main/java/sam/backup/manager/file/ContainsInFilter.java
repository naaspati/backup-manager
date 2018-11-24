package sam.backup.manager.file;

import java.util.Collection;
import java.util.function.Predicate;

import sam.backup.manager.file.db.FileImpl;
import sam.collection.IntSet;

public class ContainsInFilter implements Predicate<FileImpl> {
	private IntSet files;
	private IntSet dirs;
	
	public ContainsInFilter(Collection<? extends FileImpl> containsIn) {
		if(containsIn.isEmpty()) {
			return;
		}
		
		dirs = new IntSet();
		files = new IntSet();
		
		for (FileImpl f : containsIn) {
			if(f.isDirectory())
				dirs.add(f.id);
			else
				files.add(f.id);
			
			while((f = f.getParent()) != null) dirs.add(f.id);
		}
	}

	@Override
	public boolean test(FileImpl f) {
		if(files == null && dirs == null)
			return false;
		
		if(f.isDirectory())
			return dirs.contains(f.id);
		else 
			return files.contains(f.id);
	}
}
