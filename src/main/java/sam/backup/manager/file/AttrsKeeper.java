package sam.backup.manager.file;

public class AttrsKeeper {
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
		old = current;
	}
	public boolean isModified() {
		return old == null || old.modifiedTime != current.modifiedTime;
	}
	void setCurrentSize(long size) {
		current = new Attrs(current.modifiedTime, size);
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
}
