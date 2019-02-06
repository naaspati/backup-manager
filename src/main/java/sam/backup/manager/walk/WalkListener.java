package sam.backup.manager.walk;

import sam.backup.manager.file.Dir;
import sam.backup.manager.file.FileEntity;

public interface WalkListener {
	public void onFileFound(FileEntity ft, long size, WalkMode mode);
	public void onDirFound(Dir ft, WalkMode mode);
}
