package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import sam.backup.manager.extra.TreeType;
import sam.myutils.Checker;
import sam.nopkg.Junk;

final class FileTreeImpl implements FileTree {
	private TreeType treetype;
	private final Path srcPath;
	private Path backupPath;

	FileTreeImpl(TreeType type, Path sourceDirPath, Path backupDirPath, int child_count) throws IOException {
		Checker.requireNonNull(
				"type sourceDirPath backupDirPath",  
				type,
				sourceDirPath,
				backupDirPath
				);

		this.treetype = type;
		this.srcPath = sourceDirPath;
		this.backupPath = backupDirPath;
	}
	FileTreeImpl(TreeType type, Path sourceDirPath, Path backupDirPath) throws IOException {
		this(type, sourceDirPath, backupDirPath, 0);
	}
	private String srcPathString, backupPathString;
	
	public String getSourcePath() { 
		return srcPathString != null ? srcPathString : ( srcPathString = srcPath.toString()); 
	}
	
	public String getBackupPath() {
		return backupPathString != null ? backupPathString : ( backupPathString = backupPath == null ? "" : backupPath.toString());
	}
	public TreeType getTreetype(){ return this.treetype; }

	public void forcedMarkUpdated() {
		//TODO serializer.applyToAll(f -> f.getSourceAttrs().setUpdated());
		Junk.notYetImplemented();
	}
	public void save() throws IOException {
		//TODO serializer.save();
		Junk.notYetImplemented();
		
	}
}