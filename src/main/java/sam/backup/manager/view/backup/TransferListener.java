package sam.backup.manager.view.backup;

import javafx.concurrent.Worker.State;
import sam.backup.manager.file.api.FileEntity;

interface TransferListener {
	void subProgress(FileEntity ft, long read, long total);
	void totalProgress(long read, long total);
	void stateChanged(State s);
	void generalEvent(Type type);
	void generalEvent(Type type, Type subtype, Object attachment);
	void start(Type type, FileEntity f);
	void success(Type type, FileEntity f);
	void completed(Type type, FileEntity f);
	void failed(Type type, FileEntity f, Throwable e);
}
