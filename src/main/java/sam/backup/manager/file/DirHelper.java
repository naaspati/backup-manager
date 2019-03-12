package sam.backup.manager.file;

import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.Status;
import sam.backup.manager.file.api.Type;

public interface DirHelper {

	Status statusOf(int id);
	Attr attr(int id, Type type);
	FileEntity file(int id);

}
