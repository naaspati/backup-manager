package sam.backup.manager.file;

import sam.backup.manager.file.api.FileEntity;

interface FileEntityWithId extends FileEntity {
	int getId();
	
	public static int id(FileEntity f) {
		return ((FileEntityWithId)f).getId();
	}
}
