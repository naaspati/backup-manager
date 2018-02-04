package sam.backup.manager.extra;

public class OldNewLong {
	private final long old, _new;

	public OldNewLong(long old, long _new) {
		super();
		this.old = old;
		this._new = _new;
	}
	public long getOld() {
		return old;
	}
	public long getNew() {
		return _new;
	}
	public long difference() {
		return _new - old;
	}
}
