package sam.backup.manager.file;

import java.nio.file.Path;

import sam.backup.manager.file.FileTreeReader.Values;

public class FileEntity extends FileTreeEntity {
	FileEntity(Path path, DirEntity parent) {
		super(path, parent);
	}
	FileEntity(Values v, DirEntity parent) {
		super(v, parent);

		if(v.isDirectory())
			throw new IllegalArgumentException("not a file: "+v);
	}
	@Override
	public boolean isDirectory() {
		return false;
	}
}
