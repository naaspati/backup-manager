package sam.backup.manager.file.api;

import java.util.function.Predicate;

public interface FilteredDir extends Dir {
    Predicate<FileEntity> ALWAYS_TRUE = s -> true; 
    
	FilteredDir getParent() ;

}
