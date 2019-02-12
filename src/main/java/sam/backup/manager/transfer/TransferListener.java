package sam.backup.manager.transfer;

import sam.backup.manager.config.PathWrap;

interface TransferListener {
	void copyStarted(PathWrap src, PathWrap target);
	void copyCompleted(PathWrap src, PathWrap target);
	void addBytesRead(long bytes);
	void newTask();
}
