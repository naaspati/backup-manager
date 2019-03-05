package sam.backup.manager.view.backup;

import java.io.IOException;

import sam.backup.manager.file.api.FileEntity;

class FileEntityException extends IOException {
	private static final long serialVersionUID = 1L;
	
	public transient final FileEntity forFile;
	
	public FileEntityException(FileEntity forFile, IOException e) {
		super(e);
		this.forFile = forFile;
	}

	public FileEntityException(FileEntity forFile, String string) {
		super(string);
		this.forFile = forFile;
	}

	public FileEntityException(FileEntity ft, String string, IOException e) {
		super(string, e);
		this.forFile = ft;
	}
}
