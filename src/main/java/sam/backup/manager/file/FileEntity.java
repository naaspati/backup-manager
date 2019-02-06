package sam.backup.manager.file;

public interface FileEntity {
	int getId();
	Dir getParent();
	Attrs getBackupAttrs();
	Attrs getSourceAttrs();
	boolean isDirectory();
	Status getStatus();
	String getName();
	String getSourcePath();
	String getBackupPath();
	void delete();
	boolean remove();
}
