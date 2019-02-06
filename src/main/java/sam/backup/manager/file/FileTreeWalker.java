package sam.backup.manager.file;

import java.nio.file.FileVisitResult;

public interface FileTreeWalker {
	FileVisitResult file(FileEntity ft);
	FileVisitResult dir(Dir ft);
}
