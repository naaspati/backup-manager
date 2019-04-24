package sam.backup.manager.api;

@FunctionalInterface
public interface Stoppable {
	void stop();
	default String description() { return null; }
}
