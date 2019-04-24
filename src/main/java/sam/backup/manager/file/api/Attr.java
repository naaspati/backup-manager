package sam.backup.manager.file.api;

import sam.myutils.MyUtilsBytes;

public class Attr {
    public static final int BYTES = Long.BYTES * 2; 
    
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
		return "Attr [lastModified=" + lastModified + ", size=" + size+"("+MyUtilsBytes.bytesToHumanReadableUnits(size, false) +")]";
	}
	public long size() {
		return size;
	}
	public long lastModified() {
		return lastModified;
	}
}
 
