package sam.backup.manager.file.api;

public class Attr {
	public final long lastModified;
	public final int size;
	
	public Attr(long lastModified, int size){
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
	public int size() {
		return size;
	}
	public long lastModified() {
		return lastModified;
	}
}
 
