package sam.backup.manager.file;

public class Attr {
	public final long lastModified;
	public final long size;
	
	public Attr(long lastModified, long size){
		this.lastModified = lastModified;
		this.size = size;
	}
	public Attr(Attr from){
		this.lastModified = from.lastModified;
		this.size = from.size;
	}
	
	@Override
	public String toString() {
		return "Attr [lastModified=" + lastModified + ", size=" + size + "]";
	}
	public long size() {
		return size;
	}
	public long lastModified() {
		return lastModified;
	}
}
 
