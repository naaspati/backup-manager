package sam.backup.manager.file;

import java.util.Objects;

import sam.nopkg.Junk;

public class FileImpl implements FileEntity {
	protected final int id;
	protected final Dir parent;
	protected final String filename;
	protected final Attrs srcAttrs, backupAttrs; // direct 

	FileImpl(int id, Dir parent, String filename, Attrs source, Attrs backup) {
		this.id = id;
		this.parent = parent;
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
	
	private String sourcePath;
	@Override
	public String getSourcePath() {
		if(sourcePath == null)
			sourcePath = concat(parent.getSourcePath(), filename);
		return sourcePath;
	}
	private String backupPath;
	@Override
	public String getBackupPath() {
		if(backupPath == null)
			backupPath = concat(parent.getBackupPath(), filename);
		return backupPath;
	}
	private static final StringBuilder sb = new StringBuilder();
	
	private String concat(String s, String t) {
		synchronized (sb) {
			sb.setLength(0);
			return sb.append(s).append('\\').append(t).toString();
		}
	}

	public Status getStatus() {
		// FIXME Auto-generated method stub
		Junk.notYetImplemented();
		return null;
	}
	@Override
	public void delete() {
		// FIXME Auto-generated method stub
				Junk.notYetImplemented();
	}
}

