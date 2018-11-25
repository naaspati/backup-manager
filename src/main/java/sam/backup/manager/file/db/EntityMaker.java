package sam.backup.manager.file.db;

public interface EntityMaker<F extends FileEntity, D extends Dir> {
	F newFile(D dir, String filename);
	D newDir(D dir, String filename);
}
