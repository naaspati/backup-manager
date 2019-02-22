package sam.backup.manager.view.backup;

import java.io.IOException;
import java.nio.file.Path;

import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;

class DirCreationFailedException extends FileEntityException {
	private static final long serialVersionUID = 1L;
	
	public transient final Path path;
	
	public DirCreationFailedException(Path path, FileEntity forFile, IOException e) {
		super(forFile, e);
		this.path = path;
	}
}
