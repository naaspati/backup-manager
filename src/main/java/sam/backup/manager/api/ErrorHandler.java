package sam.backup.manager.api;

@FunctionalInterface
public interface ErrorHandler {
    void handleError(Object msg, Object header, Throwable thrown);
}
