package sam.backup.manager.file;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.function.Predicate;

public class ContainsInFilter implements Predicate<FileTreeEntity> {
	IdentityHashMap<FileTreeEntity, Void> map = new IdentityHashMap<>();
	
	public ContainsInFilter(Collection<? extends FileTreeEntity> containsIn) {
		if(containsIn.isEmpty())
			return;
		
		for (FileTreeEntity f : containsIn) {
			map.put(f, null);
			while((f = f.getParent()) != null) map.put(f, null);
		}
	}

	@Override
	public boolean test(FileTreeEntity f) {
		if(f instanceof FilteredDirEntity && map.containsKey(((FilteredDirEntity)f).getDir()))
			return true;
		return map.containsKey(f);
	}

}
