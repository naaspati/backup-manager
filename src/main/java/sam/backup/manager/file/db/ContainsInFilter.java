package sam.backup.manager.file.db;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ContainsInFilter implements Predicate<FileEntity> {
	private Map<FileEntity, Void> files;
	private Map<FileEntity, Void> dirs;
	
	public ContainsInFilter(Collection<? extends FileEntity> containsIn) {
		if(containsIn.isEmpty()) {
			return;
		}
		
		dirs = new IdentityHashMap<>();
		files = new IdentityHashMap<>();
		
		for (FileEntity f : containsIn) {
			if(f.isDirectory())
				dirs.put(f, null);
			else
				files.put(f, null);
			
			while((f = f.getParent()) != null) dirs.put(f, null);
		}
		
		if(dirs.isEmpty())
			dirs = Collections.emptyMap();
		if(files.isEmpty())
			files = Collections.emptyMap();
	}

	@Override
	public boolean test(FileEntity f) {
		if(files == null && dirs == null)
			return false;
		
		if(f.isDirectory())
			return dirs.containsKey(f);
		else 
			return files.containsKey(f);
	}
}
