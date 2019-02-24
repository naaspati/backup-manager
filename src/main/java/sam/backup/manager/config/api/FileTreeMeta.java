package sam.backup.manager.config.api;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.FileTree;

public interface FileTreeMeta {
	public PathWrap getSource();
	public PathWrap getTarget();
	public FileTree getFileTree();
}
