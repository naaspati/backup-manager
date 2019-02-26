package sam.backup.manager.file.api;

import java.nio.file.Path;

import sam.backup.manager.config.api.Config;

public interface FileTreeManager {
	void walk(Dir start, FileTreeWalker walker) ;
	void save(Config config, Path saveDir, FileTree filetree) throws Exception;
	FileTree read(Config config, Path saveDir, boolean createNewIfNotExists);
}
