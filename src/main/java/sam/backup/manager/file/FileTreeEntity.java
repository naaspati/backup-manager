package sam.backup.manager.file;

import static sam.backup.manager.walk.WalkMode.BACKUP;

import java.nio.file.Path;
import java.nio.file.Paths;

import sam.backup.manager.config.RootConfig;
import sam.backup.manager.file.FileTreeReader.Values;
import sam.backup.manager.walk.WalkMode;

public abstract class FileTreeEntity {
	private String fileNameString;

	private Path fileName; // lazy initiated  
	private Path fullPath;
	private Path target;
	private final DirEntity parent;

	private final AttrsKeeper sourceAttrs;
	private final AttrsKeeper backupAttrs;

	FileTreeEntity(Path path, DirEntity parent) {
		this.fileName = path;
		this.sourceAttrs = new AttrsKeeper();
		this.backupAttrs = new AttrsKeeper();
		this.parent = parent;
	}
	FileTreeEntity(Values values, DirEntity parent) {
		this.fileNameString = values.getFileNameString();
		this.sourceAttrs = new AttrsKeeper(values.sourceAttrs());
		this.backupAttrs = new AttrsKeeper(values.backupAttrs());
		this.parent = parent;
	}
	
	public abstract boolean isDirectory();
	public abstract boolean isCopied() ;
	public abstract boolean isBackupNeeded() ;
	public abstract boolean isDeleteFromBackup() ;

	public DirEntity getParent() {
		return parent;
	}
	DirEntity castDir() {
		return (DirEntity)this;
	}
	FileEntity castFile() {
		return (FileEntity)this;
	}
	void setTarget(Path target) {
		this.target = target;
	}
	public Path getTargetPath() {
		return target;
	}
	public Path getFileName() {
		if(fileName != null)
			return fileName;
		return fileName = Paths.get(fileNameString); 
	}
	public String getfileNameString() {
		if(fileNameString == null)
			fileNameString = fileName.toString();

		return fileNameString;
	}
	public Path getSourcePath() {
		return fullPath;
	}
	public AttrsKeeper getBackupAttrs() {
		return backupAttrs;
	}
	public AttrsKeeper getSourceAttrs() {
		return sourceAttrs;
	}
	void setAttrs(Attrs attr, WalkMode walkType, Path fullpath) {
		if(walkType == BACKUP)
			backupAttrs.setCurrent(attr);
		else {
			sourceAttrs.setCurrent(attr);
			this.fullPath = fullpath;
		}
	}
	void computeTargetPath(final Path parent) {
		if(RootConfig.backupDriveFound())
			setTarget(parent.resolve(getFileName()));
	}

	@Override
	public String toString() {
		return getfileNameString();
	}
	protected void setUpdated() {
		getBackupAttrs().setUpdated();
		getSourceAttrs().setUpdated();
	}
}