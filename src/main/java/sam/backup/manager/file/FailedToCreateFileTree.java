package sam.backup.manager.file;

import java.io.IOException;

class FailedToCreateFileTree extends IOException {
	private static final long serialVersionUID = -5162419308441790067L;
	
	public FailedToCreateFileTree(String msg) {
		super(msg);
	}
	public FailedToCreateFileTree() { }
	
	
}
