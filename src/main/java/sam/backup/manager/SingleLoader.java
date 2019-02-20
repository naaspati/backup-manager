package sam.backup.manager;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface SingleLoader {

	public static <E, F> E load(Class<E> cls) {
		return load(cls, null);
	}

	public static <E, F extends E> E load(Class<E> cls, Class<F> defaultImpl) {
		Iterator<E> itr = ServiceLoader.load(cls).iterator();
		if(!itr.hasNext()) {
			if(defaultImpl == null)
				throw new IllegalStateException("no implementation found for: "+cls);
			else {
				try {
					return defaultImpl.newInstance();
				} catch (InstantiationException | IllegalAccessException e1) {
					throw new RuntimeException(e1);
				}
			}
		}

		E e = itr.next();

		if(itr.hasNext())
			throw new IllegalStateException("more than 1 implementation found for: "+cls);

		return e;
	}
}
