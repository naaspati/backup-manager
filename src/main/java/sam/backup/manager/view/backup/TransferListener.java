package sam.backup.manager.view.backup;

import sam.backup.manager.file.api.FileEntity;

interface TransferListener {
	void notify(Type type, Object attached);
	void notify(Type type, int attached);
	void notify(Type type, double attached);
	void notify(Type type);
	void notify(Type failedZipDir, Object attached, Object attached2);
	void bytesMoved(FileEntity f, int bytesCount);
	void subProgress(FileEntity ft, long read, long total);
	void totalProgress(long read, long total);
}
