package sam.backup.manager.extra;

public interface IStartOnComplete<E> {
	public void start(E e);
	public void onComplete(E e);
}
