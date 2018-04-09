package sam.backup.manager.file;

import java.nio.file.FileVisitResult;

public abstract class SimpleFileTreeWalker implements FileTreeWalker {
	@Override
	public FileVisitResult file(FileEntity ft) {
		return FileVisitResult.CONTINUE;
	}
	@Override
	public FileVisitResult dir(DirEntity ft) {
		return FileVisitResult.CONTINUE;
	}
}