package sam.backup.manager.file;

import java.io.IOException;

import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.Type;

public interface FileTreeDeleter extends AutoCloseable {
	@Override void close() throws IOException;
	void delete(FileEntity fte, Type type)  throws IOException;
}
