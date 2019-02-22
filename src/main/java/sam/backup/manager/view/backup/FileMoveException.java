package sam.backup.manager.view.backup;

import java.io.IOException;
import java.nio.file.Path;

import sam.backup.manager.file.api.FileEntity;

class FileMoveException extends FileEntityException {
	public final Path src;
	public Path target;

	public FileMoveException(FileEntity ft, Path src, Path target, IOException e) {
		super(ft, e);
		this.src = src;
		this.target = target;
	}
}
