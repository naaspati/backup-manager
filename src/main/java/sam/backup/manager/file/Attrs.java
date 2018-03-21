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
	
	
	
	

}
