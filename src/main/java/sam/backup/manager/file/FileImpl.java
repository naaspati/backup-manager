package sam.backup.manager.file;

import java.util.Objects;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.Status;
import sam.backup.manager.file.api.Type;

public class FileImpl implements FileEntity {
	
	private final FileHelper helper;

	public final int id;
	
	private final DirImpl parent;
	public final String filename;
	private Attrs srcAttrs, backupAttrs; // direct
	private PathWrap srcPath;
	private PathWrap backupPath;
	private Status status;
	
	// to used by FileTree
	protected FileImpl(int id, String filename, DirImpl parent, FileHelper helper) {
		this.id = id;
		this.filename = Objects.requireNonNull(filename);
		this.helper = helper;
		this.parent = parent;
		
		if(!(this instanceof FileTree))
			throw new IllegalAccessError("can on be accessed by FileTree");
	}
	
	@Override
	public DirImpl getParent() {
		return parent;
	}
	
	@Override
	public Status getStatus() {
		if(status == null)
			status = fileHelper().statusOf(this);
		
		return status;
	}

	protected Attr attr(Type type) {
		return fileHelper().attr(this, type);
	}
	
	protected FileHelper fileHelper() {
		return helper;
	}
	@Override
	public boolean isDirectory() {
		return false;
	}
	@Override
	public Attrs getAttrs(Type type) {
		switch (type) {
			case BACKUP: return getBackupAttrs();
			case SOURCE: return getSourceAttrs();
			default:
				throw new NullPointerException();
		}
	}
	
	
	public Attrs getSourceAttrs() {
		if(srcAttrs == null)
			srcAttrs = new Attrs(attr(Type.BACKUP));
		return srcAttrs;
	}
	public Attrs getBackupAttrs() {
		if(backupAttrs == null)
			backupAttrs = new Attrs(attr(Type.BACKUP));
		return backupAttrs;
	}
	
	@Override
	public String getName() {
		return filename;
	}
	
	@Override
	public PathWrap getPath(Type type) {
		switch (type) {
			case BACKUP: return getBackupPath();
			case SOURCE: return getSourcePath();
			default:
				throw new NullPointerException();
		}
	}
	
	public PathWrap getSourcePath() {
		if(srcPath == null)
			srcPath = getParent().getPath(Type.SOURCE).resolve(filename);
		return srcPath;
	}
	public PathWrap getBackupPath() {
		if(backupPath == null)
			backupPath = getParent().getPath(Type.BACKUP).resolve(filename);
		return backupPath;
	}
	
	@Override
	public long getSourceSize() {
		if(srcAttrs == null) {
			Attr a = attr(Type.SOURCE);
			if(a == null)
				return 0;
			return a.size;
		}
			
		Attrs a = getAttrs(Type.SOURCE); 
		return a.size();
	}
	@Override
	public int hashCode() {
		return id;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		FileImpl other = (FileImpl) obj;
		return id == other.id;
	}
	@Override
	public String toString() {
		return "FileImpl [dir_id=" + getParent() + ", filename=" + filename + "]";
	}
	
}


