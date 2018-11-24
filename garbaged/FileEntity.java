package sam.backup.manager.file;

import java.nio.file.Path;

public class FileEntity extends FileTreeEntity {
	FileEntity(Path path, DirEntity parent) {
		super(path, parent);
	}
	public FileEntity(String fileNameString, DirEntity parent, Attrs sourceAttr, Attrs backupAttr) {
		super(fileNameString, parent, sourceAttr, backupAttr);
	}
	@Override
	public boolean isDirectory() {
		return false;
	}
	@Override
	public void setCopied(boolean b) {
		super.setCopied(b);
		markUpdated();
	}
}

