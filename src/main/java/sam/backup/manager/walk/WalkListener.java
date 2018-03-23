package sam.backup.manager.walk;

import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;

public interface WalkListener {

	public void walkCompleted();
	public void walkFailed(String reason, Throwable e);
	public void onFileFound(FileEntity ft, long size, WalkMode mode);
	public void onDirFound(DirEntity ft, WalkMode mode);
}
