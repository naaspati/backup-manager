package sam.backup.manager.file.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import sam.backup.manager.config.api.Config;

public interface FileTreeManager {
	void walk(Dir start, FileTreeWalker walker) ;
	void save(Config config, Path saveDir, FileTree filetree) throws Exception;
	FileTree read(Config config, Path saveDir, boolean createNewIfNotExists);
	
	default void writeFileTreeAsString(Dir dir, Appendable sink) throws IOException  {
		writeFileTreeAsString(dir, null, sink);
	}
	void writeFileTreeAsString(Dir dir, Predicate<FileEntity> filter, Appendable sink) throws IOException ;
}
