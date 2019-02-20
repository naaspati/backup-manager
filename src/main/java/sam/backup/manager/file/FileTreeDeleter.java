package sam.backup.manager.file;

import java.io.IOException;

import sam.backup.manager.config.api.PathWrap;
import sam.backup.manager.file.api.FileEntity;

public interface FileTreeDeleter extends AutoCloseable {
	@Override void close() throws IOException;
	void delete(FileEntity fte, PathWrap file)  throws IOException;
}
