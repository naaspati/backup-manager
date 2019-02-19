package sam.backup.manager.file;

import java.io.IOException;
import java.util.function.BiConsumer;

import sam.backup.manager.config.PathWrap;
import sam.backup.manager.file.api.FileEntity;

public interface FileTreeDeleter extends AutoCloseable {
	@Override void close() throws IOException;
	void delete(FileEntity fte, PathWrap file)  throws IOException;
}
