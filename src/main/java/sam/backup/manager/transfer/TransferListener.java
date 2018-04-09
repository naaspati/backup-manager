package sam.backup.manager.transfer;

import java.nio.file.Path;

interface TransferListener {
	void copyStarted(Path src, Path target);
	void copyCompleted(Path src, Path target);
	void addBytesRead(long bytes);
	void newTask();
}
