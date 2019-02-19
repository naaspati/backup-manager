package sam.backup.manager.file;

import static sam.backup.manager.file.FileEntityWithId.id;

import java.util.Collection;
import java.util.function.Predicate;

import sam.backup.manager.file.api.FileEntity;
import sam.collection.IntSet;
import sam.myutils.Checker;

public final class FileUtils {
	@SuppressWarnings("rawtypes")
	private static final Predicate ALWAYS_FALSE = e -> false;
	
	private FileUtils() { }
	
	@SuppressWarnings("unchecked")
	public static Predicate<FileEntity> containsInFilter(Collection<? extends FileEntity> containsIn) {
		if(Checker.isEmpty(containsIn))
			return ALWAYS_FALSE;
		
		else if(containsIn.stream().allMatch(e -> e instanceof FileEntityWithId))
			return withIdFilter(containsIn);
		else 
			return (f -> containsIn.contains(f));
		
	}
	private static Predicate<FileEntity> withIdFilter(Collection<? extends FileEntity> containsIn) {
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
		return f -> f == null ? false : (f.isDirectory() ? dirs : files).contains(id(f));
	}
}
