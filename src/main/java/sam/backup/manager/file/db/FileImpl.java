package sam.backup.manager.file.db;

import java.util.Objects;

class FileImpl implements FileEntity {
	protected final Dir parent;
	protected final String filename;
	protected final Attrs srcAttrs, backupAttrs; // direct 

	FileImpl(Dir parent, String filename, Attrs source, Attrs backup) {
		this.parent = parent;
		this.filename = Objects.requireNonNull(filename);
		this.srcAttrs = source;
		this.backupAttrs = backup; 
	}

	public Dir getParent() {
		return parent;
	}
	public boolean isDirectory() {
		return false;
	}
	public Attrs getSourceAttrs() {
		return srcAttrs;
	}
	public Attrs getBackupAttrs() {
		return backupAttrs;
	}
	@Override
	public String toString() {
		return "FileImpl [dir_id=" + parent + ", filename=" + filename + "]";
	}
	public String getName() {
		return filename;
	}
	
	private String sourcePath;
	public String getSourcePath() {
		if(sourcePath == null)
			sourcePath = concat(parent.getSourcePath(), filename);
		return sourcePath;
	}
	private String backupPath;
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
		return null;
	}
}

