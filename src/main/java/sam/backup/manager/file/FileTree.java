package sam.backup.manager.file;

import java.io.IOException;

public interface FileTree {
	void save() throws IOException;
	void forcedMarkUpdated();
}
