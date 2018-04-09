package sam.backup.manager.file;

import java.nio.file.FileVisitResult;

public interface FileTreeWalker {
	public FileVisitResult file(FileEntity ft);
	public FileVisitResult dir(DirEntity ft);
}
