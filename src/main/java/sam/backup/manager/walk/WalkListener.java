package sam.backup.manager.walk;

import java.nio.file.Path;

import javafx.concurrent.Worker.State;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;

public interface WalkListener {
	public void onFileFound(FileEntity ft, long size, WalkMode mode);
	public void onDirFound(Dir ft, WalkMode mode);
	public void stateChange(State s);
	public void failed(String msg, Throwable error);
	public void startWalking(Path path);
	public void endWalking(Path path);
}
