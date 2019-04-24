package sam.backup.manager.file.api;

import java.util.Objects;

import sam.backup.manager.config.impl.PathWrap;

public abstract class AbstractFileImpl implements FileEntity {
	
	public final String filename;
	private Attrs srcAttrs, backupAttrs; // direct
	private PathWrap srcPath;
	private PathWrap backupPath;
	
	// to used by FileTree
	protected AbstractFileImpl(String filename) {
		this.filename = Objects.requireNonNull(filename);
	}

	protected abstract Attr attr(Type type);
	
	@Override
	public boolean isDirectory() {
		return false;
	}
	
	@Override
	public Attrs getSourceAttrs() {
		if(srcAttrs == null)
			srcAttrs = new Attrs(attr(Type.BACKUP));
		return srcAttrs;
	}
	@Override
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
	public PathWrap getSourcePath() {
		if(srcPath == null)
			srcPath = getParent().getSourcePath().resolve(filename);
		return srcPath;
	}
	@Override
	public PathWrap getBackupPath() {
		if(backupPath == null)
			backupPath = getParent().getBackupPath().resolve(filename);
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
			
		Attrs a = getSourceAttrs(); 
		return a.size();
	}
}


