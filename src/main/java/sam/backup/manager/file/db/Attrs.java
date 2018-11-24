package sam.backup.manager.file.db;

import static sam.backup.manager.file.db.FileTree.DEFAULT_ATTR;
public class Attrs {
	private final Attr old;
	private Attr current, nnew;
	
	public Attrs(Attr old) {
		this.old = old;
		this.current = old;
		this.nnew = FileTree.DEFAULT_ATTR;
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
		return current != DEFAULT_ATTR ? current.size : old == DEFAULT_ATTR ? -1 : old.size;
	}
}
