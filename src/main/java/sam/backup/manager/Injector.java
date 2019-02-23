package sam.backup.manager;

import java.lang.annotation.Annotation;

public interface Injector {
	public <E> E instance(Class<E> type);
	public <E, F extends Annotation> E instance(Class<E> type, Class<F> qualifier);
}
