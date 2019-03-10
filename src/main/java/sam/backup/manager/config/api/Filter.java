package sam.backup.manager.config.api;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

@FunctionalInterface
public interface Filter extends Predicate<Path> { 
	default Map<String, Object> toMap() { return null; } 

}
