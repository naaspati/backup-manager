package sam.backup.manager.file.db;

import java.util.Objects;

public class FileImpl {
	private final int id;
	private final Dir parent;
	private final String filename;
	private final Attrs srcAttrs, backupAttrs; // direct 

	FileImpl(int id, Dir parent, String filename, Attrs source, Attrs backup) {
		this.id = id;
		this.parent = parent;
		this.filename = Objects.requireNonNull(filename);
		this.srcAttrs = source;
		this.backupAttrs = backup; 
	}

	public int getId(){ return this.id; }
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
	
	static int id(FileImpl item, Attrs a) {
		if(a.current().id < 0)
			throw new IllegalArgumentException("Attrs not persisted yet: "+item+"  "+a);
		return a.current().id;
	}
	@Override
	public final int hashCode() {
		return id;
	}
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		FileImpl other = (FileImpl) obj;
		if (id != other.id)
			return false;
		if(other != this) throw new IllegalArgumentException("two different Entity have same id: "+this+"  "+other);
		return true;
	}
	@Override
	public String toString() {
		return "FileImpl [id=" + id + ", dir_id=" + parent + ", filename=" + filename + "]";
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

