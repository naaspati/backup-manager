package sam.backup.manager.file;

public interface Generator {
	FileEntity newFile(String filename);
	Dir newDir(String dirname);
}
