package sam.backup.manager.walk;

import sam.backup.manager.file.db.Dir;
import sam.backup.manager.file.db.FileImpl;

public interface WalkListener {

	public void walkCompleted();
	public void walkFailed(String reason, Throwable e);
	public void onFileFound(FileImpl ft, long size, WalkMode mode);
	public void onDirFound(Dir ft, WalkMode mode);
}
