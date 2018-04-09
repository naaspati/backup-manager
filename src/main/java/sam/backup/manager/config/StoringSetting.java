package sam.backup.manager.config;

import sam.backup.manager.config.filter.Filter;
import sam.backup.manager.config.filter.IFilter;

public class StoringSetting {
	public transient static final StoringSetting DIRECT_COPY = new StoringSetting();
	
	private StoringMethod method;
	private Filter selector;
	
	public StoringMethod getMethod() {
		return method == null ? StoringMethod.NONE : method;
	}
	public IFilter getSelecter() {
		return selector == null ? (p -> false) : selector;
	}
}
