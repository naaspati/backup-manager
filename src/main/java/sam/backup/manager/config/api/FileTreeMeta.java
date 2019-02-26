package sam.backup.manager.config.api;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;

public interface FileTreeMeta {
	PathWrap getSource();
	PathWrap getTarget();
	FileTree getFileTree();
	long getLastModified(); //FIXME confisuing name
	FileTree loadFiletree(FileTreeManager manager, boolean createNewIfNotExists) throws Exception;
}
