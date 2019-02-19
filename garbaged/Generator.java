package sam.backup.manager.file;

import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;

interface Generator {
	FileEntity newFile(String filename);
	Dir newDir(String dirname);
}
