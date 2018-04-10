package sam.backup.manager.walk;

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
	public void clear() {
		needed = false;
		reason = null;
	}

	@Override
	public String toString() {
		return new StringBuilder()
		.append("BackupNeeded [needed=")
		.append(needed)
		.append(", reason=")
		.append(reason)
		.append("]").toString();
	}
	
}
