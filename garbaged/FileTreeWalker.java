package sam.backup.manager.file;

import java.nio.file.FileVisitResult;

public interface FileTreeWalker {
	public FileVisitResult file(FileImpl ft);
	public FileVisitResult dir(DirEntity ft);
}
