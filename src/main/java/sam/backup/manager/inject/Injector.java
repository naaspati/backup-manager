package sam.backup.manager.inject;

import java.lang.annotation.Annotation;

public interface Injector {
	public <E> E instance(Class<E> type);
	public <E, F extends Annotation> E instance(Class<E> type, Class<F> qualifier);
	
}
