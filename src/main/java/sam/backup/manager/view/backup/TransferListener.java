package sam.backup.manager.view.backup;

import sam.backup.manager.config.api.PathWrap;

interface TransferListener {
	void copyStarted(PathWrap src, PathWrap target);
	void copyCompleted(PathWrap src, PathWrap target);
	void addBytesRead(long bytes);
	void newTask();
}
