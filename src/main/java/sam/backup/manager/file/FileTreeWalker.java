package sam.backup.manager.file;

import java.nio.file.FileVisitResult;

public interface FileTreeWalker {
	public FileVisitResult file(FileEntity ft, AttrsKeeper source, AttrsKeeper backup);
	public FileVisitResult dir(DirEntity ft, AttrsKeeper source, AttrsKeeper backup);
}
