package sam.backup.manager.file.db;

public class Attrs {
	private final Attr old;
	private Attr current, nnew;
	
	public Attrs(Attr old) {
		this.old = old;
		this.current = old;
		this.nnew = old;
	}

	public Attr current() {
		return current;
	}
	void setCurrent(Attr current) {
		this.current = current;
	}
	public Attr old() {
		return old;
	}
	public Attr neW() {
		return nnew;
	}
	public boolean isModified() {
		return old != current;
	}

	public void setUpdated() {
		current = nnew;
	}
	public long size() {
		return !isDefault(current) ? current.size : isDefault(old) ? -1 : old.size;
	}
	private boolean isDefault(Attr current2) {
		return current == null || current.lastModified <= 0;
	}

	@Override
	public String toString() {
		return "Attrs [old=" + old + ", current=" + current + ", nnew=" + nnew + "]";
	}
	
}
