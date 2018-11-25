package sam.backup.manager.file.db;

public interface FileEntity {
	Dir getParent();
	Attrs getBackupAttrs();
	Attrs getSourceAttrs();
	boolean isDirectory();
	Status getStatus();
	String getName();
	String getSourcePath();
	String getBackupPath();
}
