package sam.backup.manager.file;

import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Status;
import sam.backup.manager.file.api.Type;

public interface FileHelper {
	Status statusOf(FileImpl file);
	Attr attr(FileImpl file, Type type);
}
