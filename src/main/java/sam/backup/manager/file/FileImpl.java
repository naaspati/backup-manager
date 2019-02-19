package sam.backup.manager.file;

import java.util.Objects;

import sam.backup.manager.config.PathWrap;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.nopkg.Junk;

public class FileImpl implements FileEntity {
	protected final int id;
	protected final DirImpl parent;
	protected final String filename;
	protected final Attrs srcAttrs, backupAttrs; // direct
	protected PathWrap sourcePath;
	protected PathWrap backupPath;
	
	// to used by FileTree
	protected FileImpl(int id, String filename, Attrs source, Attrs backup) {
		this.id = id;
		this.parent = null;
		this.filename = Objects.requireNonNull(filename);
		this.srcAttrs = source;
		this.backupAttrs = backup;
		
		if(!(this instanceof FileTree))
			throw new IllegalAccessError("can on be accessed by FileTree");
	}
	FileImpl(int id, DirImpl parent, String filename, Attrs source, Attrs backup) {
		this.id = id;
		this.parent = Objects.requireNonNull(parent);
		this.filename = Objects.requireNonNull(filename);
		this.srcAttrs = source;
		this.backupAttrs = backup; 
	}
	@Override
	public int getId() {
		return id;
	}
	@Override
	public Dir getParent() {
		return parent;
	}
	@Override
	public boolean isDirectory() {
		return false;
	}
	@Override
	public Attrs getSourceAttrs() {
		return srcAttrs;
	}
	@Override
	public Attrs getBackupAttrs() {
		return backupAttrs;
	}
	@Override
	public String toString() {
		return "FileImpl [dir_id=" + parent + ", filename=" + filename + "]";
	}
	@Override
	public String getName() {
		return filename;
	}
	
	@Override
	public PathWrap getSourcePath() {
		if(sourcePath == null)
			sourcePath = parent.getSourcePath().resolve(filename);
		return sourcePath;
	}
	@Override
	public PathWrap getBackupPath() {
		if(backupPath == null)
			backupPath = parent.getBackupPath().resolve(filename);
		return backupPath;
	}
	
	public Status getStatus() {
		// FIXME Auto-generated method stub
		Junk.notYetImplemented();
		return null;
	}
	@Override
	public long getSourceSize() {
		// TODO Auto-generated method stub
		return 0;
	}
}

