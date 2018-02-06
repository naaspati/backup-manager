package sam.backup.manager.file;

import java.nio.file.FileVisitResult;

public interface FileTreeWalker {
	public FileVisitResult file(FileTree ft, AboutFile source, AboutFile backup);
	public FileVisitResult dir(FileTree ft, AboutFile source, AboutFile backup);
}
