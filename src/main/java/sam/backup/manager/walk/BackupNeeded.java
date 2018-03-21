package sam.backup.manager.walk;

import java.util.function.BooleanSupplier;

class BackupNeeded {
	private boolean needed;
	private String reason;
	
	BackupNeeded() {}
	
	public String getReason() {
		return reason;
	}
	public boolean isNeeded() {
		return needed;
	}
	public BackupNeeded test(boolean b, String reason) {
		if(needed)
			return this;
		
		if(b) {
			needed = true;
			this.reason = reason;
		}
		return this;
	}
	
	public BackupNeeded test(BooleanSupplier s, String reason) {
		if(needed)
			return this;
		
		if(s.getAsBoolean()) {
			needed = true;
			this.reason = reason;
		}
		
		return this;
	}

	public void clear() {
		needed = false;
		reason = null;
	}
}
