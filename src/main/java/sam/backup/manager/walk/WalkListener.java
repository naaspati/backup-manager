package sam.backup.manager.walk;

import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;

public interface WalkListener {
	public void onFileFound(FileEntity ft, long size, WalkMode mode);
	public void onDirFound(Dir ft, WalkMode mode);
}
