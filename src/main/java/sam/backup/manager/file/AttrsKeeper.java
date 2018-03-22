package sam.backup.manager.file;

import java.nio.file.Path;

public class AttrsKeeper {
	private Attrs old;
	private Attrs current;
	private Path path;
	
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
	public Path getPath() {
		return path;
	}
	public boolean isNew() {
		return old == null;
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
}
