package sam.backup.manager.file;

public class Serial {
	private int n;
	
	public Serial(int n) {
		this.n = n;
	}
	public int next() {
		return n++;
	}

}
