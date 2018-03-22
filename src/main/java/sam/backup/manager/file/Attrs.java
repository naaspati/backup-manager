package sam.backup.manager.file;

public class Attrs {
	final long modifiedTime;
	long size;
	
	public Attrs(long modifiedTime, long size) {
		this.modifiedTime = modifiedTime;
		this.size = size;
	}

	public long getModifiedTime() { return modifiedTime; }
	public long getSize() { return size; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (modifiedTime ^ (modifiedTime >>> 32));
		result = prime * result + (int) (size ^ (size >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		
		Attrs other = (Attrs) obj;
		return modifiedTime == other.modifiedTime && size == other.size;
	}	
}
