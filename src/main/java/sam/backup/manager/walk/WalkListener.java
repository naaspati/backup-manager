package sam.backup.manager.walk;

public interface WalkListener {

	public void walkCompleted(WalkResult result);
	public void walkFailed(String reason, Throwable e);
	public void update(Update source, Update target);
}
