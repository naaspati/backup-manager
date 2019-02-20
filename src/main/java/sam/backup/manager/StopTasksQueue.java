package sam.backup.manager;

/**
 * each runnable will be called when application stops
 * @author Sameer
 *
 */
public interface StopTasksQueue {
	public void add(Runnable runnable);
}
