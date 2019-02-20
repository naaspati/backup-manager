package sam.backup.manager.file.api;

import java.io.IOException;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.TreeType;

public interface FileTreeFactory {
	FileTree readFiletree(Config c, TreeType type, boolean createNewIfNotExists) throws IOException ;
	public boolean saveFileTree(Config config); 
	public boolean saveFileTree(FileTree fileTree) ;
	public boolean saveFileTree(Config c, FileTree fileTree);
}