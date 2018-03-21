package sam.backup.manager.walk;

public class Update {
	public final long size;
	public final int fileCount;
	public final int dirCount;
	
	public Update(long size, int fileCount, int dirCount) {
		this.size = size;
		this.fileCount = fileCount;
		this.dirCount = dirCount;
	}
	
	
	
}
