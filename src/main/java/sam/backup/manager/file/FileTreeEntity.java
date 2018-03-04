package sam.backup.manager.file;

import static sam.backup.manager.walk.WalkType.BACKUP;

import java.nio.file.Path;
import java.nio.file.Paths;

import sam.backup.manager.config.RootConfig;
import sam.backup.manager.file.FileTreeReader.Values;
import sam.backup.manager.walk.WalkType;

public abstract class FileTreeEntity {
	private final String pathString;
	private long modifiedTime = -1;
	private long size = -1;

	private Path fileName; // lazy initiating from pathString
	private Path fullPath;
	private Path target;
	private AboutFile sourceAboutFile;
	private AboutFile backupAboutFile;

	FileTreeEntity(Path path) {
		this.pathString = path.toString();
		this.fileName = path;
	}
	FileTreeEntity(Values values) {
		this.pathString = values.getPathString();
		this.modifiedTime = values.getLastModified();
		this.size = values.getSize();
	}
	public abstract boolean isBackupNeeded();
	public abstract boolean isDeleteBackup(); 
	public abstract boolean isDirectory();
	public abstract boolean isCopied();

	DirEntity castDir() {
		return (DirEntity)this;
	}
	FileEntity castFile() {
		return (FileEntity)this;
	}
	public long getSize() {
		return size;
	}
	void setSize(long size) {
		this.size = size;
	}
	public long getModifiedTime() {
		return modifiedTime;
	}
	void setModifiedTime(long modifiedTime) {
		this.modifiedTime = modifiedTime;
	}
	void update(final Path parent) {
		if(sourceAboutFile != null)
			this.size = sourceAboutFile.size;

		if(parent != null && RootConfig.backupDriveFound())
			setTarget(parent.resolve(getFileName()));
	}
	void setTarget(Path target) {
		this.target = target;
	}
	public Path getTargetPath() {
		return target;
	}
	public Path getFileName() {
		return fileName = fileName != null ? fileName : Paths.get(pathString);
	}
	public Path getSourcePath() {
		return fullPath;
	}
	public AboutFile getBackupAboutFile() {
		return backupAboutFile;
	}
	public AboutFile getSourceAboutFile() {
		return sourceAboutFile;
	}
	void setAboutFile(AboutFile aboutFile, WalkType walkType, Path fullpath) {
		if(walkType == BACKUP)
			backupAboutFile = aboutFile;
		else {
			sourceAboutFile = aboutFile ;
			this.fullPath = fullpath;
		}
	}
	public String getPathString() {
		return pathString;
	}
	@Override
	public String toString() {
		return pathString;
	}
	public long getSourceSize() {
		return sourceAboutFile.getSize();
	}
	
	public boolean isNew() {
		return modifiedTime == -1;
	}
}

