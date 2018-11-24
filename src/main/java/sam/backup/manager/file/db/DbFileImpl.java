package sam.backup.manager.file.db;

class DbFileImpl extends FileImpl {
	public final int id;

	public DbFileImpl(int id, Dir parent, String filename, Attrs source, Attrs backup) {
		super(parent, filename, source, backup);
		this.id = id;
	}
	@Override
	public final int hashCode() {
		return id;
	}
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		DbFileImpl other = (DbFileImpl) obj;
		if (id != other.id)
			return false;
		if(other != this) throw new IllegalArgumentException("two different Entity have same id: "+this+"  "+other);
		return true;
	}
	@Override
	public String toString() {
		return getClass().getSimpleName()+" [id=" + id + ", filename=" + filename + ", srcAttrs=" + srcAttrs + ", backupAttrs="
				+ backupAttrs + "]";
	}
}
