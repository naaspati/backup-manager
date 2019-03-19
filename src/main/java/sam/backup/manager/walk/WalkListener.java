package sam.backup.manager.walk;

import java.nio.file.Path;

import javafx.concurrent.Worker.State;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;

public interface WalkListener {
	void onFileFound(FileEntity ft, long size, WalkMode mode);
	void onDirFound(Dir ft, WalkMode mode);
	void stateChange(State s);
	void failed(String msg, Throwable error);
	void startWalking(Path path);
	void endWalking(Path path);
}
