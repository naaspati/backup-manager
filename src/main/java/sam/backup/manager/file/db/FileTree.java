package sam.backup.manager.file.db;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import sam.backup.manager.extra.TreeType;
import sam.myutils.Checker;

public final class FileTree extends Dir {
	private TreeType treetype;
	private final Path srcPath;
	private Path backupPath;
	private final Serializer serializer;

	FileTree(Serializer serializer, TreeType type, Path sourceDirPath, Path backupDirPath, Attrs source, Attrs backup, int child_count) throws IOException {
		super(null, null, sourceDirPath.toString(), source, backup, new ArrayList<>(child_count));
		Checker.requireNonNull(
				new String[]{"type", "sourceDirPath", "backupDirPath", "serializer"},  
				type,
				sourceDirPath,
				backupDirPath,
				serializer
				);

		this.treetype = type;
		this.srcPath = sourceDirPath;
		this.backupPath = backupDirPath;
		this.serializer = serializer;
	}
	FileTree(Serializer serializer, TreeType type, Path sourceDirPath, Path backupDirPath) throws IOException {
		this(serializer, type, sourceDirPath, backupDirPath, serializer.defaultAttrs(), serializer.defaultAttrs(), 0);
	}
	private String srcPathString, backupPathString;
	@Override 
	public String getSourcePath() { 
		return srcPathString != null ? srcPathString : ( srcPathString = srcPath.toString()); 
	}
	@Override
	public String getBackupPath() {
		return backupPathString != null ? backupPathString : ( backupPathString = backupPath == null ? "" : backupPath.toString());
	}
	public TreeType getTreetype(){ return this.treetype; }

	public void forcedMarkUpdated() {
		serializer.applyToAll(f -> f.getSourceAttrs().setUpdated());
	}
	public FileImpl newFile(Dir parent, String filename) {
		return serializer.newFile(parent, filename);
	}
	public FileImpl newDir(Dir parent, String filename) {
		return serializer.newDir(parent, filename);
	}
	public void save() {
		serializer.save();
	}
}

