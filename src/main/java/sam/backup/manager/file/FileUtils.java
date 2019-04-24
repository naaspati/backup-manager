package sam.backup.manager.file;

import static sam.backup.manager.file.api.WithId.id;

import java.util.BitSet;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.WithId;
import sam.collection.IntSet;
import sam.myutils.Checker;

@SuppressWarnings({"rawtypes", "unchecked"})
final class FileUtils {
	public static final Predicate<FileEntity> ALWAYS_TRUE = e -> true;
	private static final Predicate<FileEntity> ALWAYS_FALSE = e -> false;
	
	private static final Logger logger = LogManager.getLogger(FileUtils.class);
	
	private FileUtils() { }
	
	public static Predicate<FileEntity> containsInFilter(Collection containsIn) {
		if(Checker.isEmpty(containsIn))
			return ALWAYS_FALSE;
		
		BitSet set = new BitSet();
		int max = 0;
		
		for (Object f : containsIn) {
			if(!(f instanceof WithId))
				return containsFilter(containsIn);
			
			int id = id(f);
			if(id > max + 1000)
				return withIdFilterWithSet(containsIn);
			
			max = Math.max(id, max);
			set.set(id);
		}
		
		logger.debug("BitSet filter creted");
		
		return f -> {
			if(f instanceof WithId)
				return set.get(id(f));
			else
				return false;
		};
	}
	private static Predicate<FileEntity> containsFilter(Collection<FileEntity> containsIn) {
		logger.debug("DIRECT filter creted");
		return z -> containsIn.contains(z);
	}

	private static Predicate<FileEntity> withIdFilterWithSet(Collection<FileEntity> containsIn) {
		logger.debug("IntSet filter creted");
		
		IntSet files = new IntSet();
		IntSet dirs = new IntSet();

		for (FileEntity f : containsIn) {
			if(!(f instanceof WithId))
				return containsFilter(containsIn);
			
			if(f.isDirectory())
				dirs.add(id(f));
			else
				files.add(id(f));

			while((f = (FileEntity) f.getParent()) != null) 
				dirs.add(id(f));
		}
		
		return f -> {
			if(f instanceof WithId) {
				if(f.isDirectory())
					return dirs.contains(id(f));
				else
					return files.contains(id(f));
			} else
				return false;
		};
		
	}
}
