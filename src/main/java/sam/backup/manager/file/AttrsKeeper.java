package sam.backup.manager.file;

import java.nio.file.Path;

public class AttrsKeeper {
	private Path path;
	private Attrs old;
	private Attrs current;

	AttrsKeeper() {}
	AttrsKeeper(Attrs old) {
		this.old = old;
	}
	public Attrs getCurrent() {
		return current;
	}
	void setCurrent(Attrs current) {
		this.current = current;
	}
	void setUpdated() {
		if(current != null)
			old = current;
	}
	public boolean isModified() {
		return old == null || old.modifiedTime != current.modifiedTime;
	}
	void setCurrentSize(long size) {
		current = new Attrs(current.modifiedTime, size);
	}
	void setPath(Path path) {
		this.path = path;
	}
	Path getPath() {
		return path;
	}
	public long getSize() {
		return current != null ? current.size : old != null ? old.size : 0;
	}
	public Attrs getOld() {
		return old;
	}
	void set(Attrs current, Path fullpath) {
		this.current = current;
		this.path = fullpath;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AttrsKeeper [");
		if (path != null) {
			sb.append("path=");
			sb.append(path);
			sb.append(", ");
		}
		if (old != null) {
			sb.append("old=");
			sb.append(old);
			sb.append(", ");
		}
		if (current != null) {
			sb.append("current=");
			sb.append(current);
		}
		sb.append("]");
		return sb.toString();
	}
}
