package sam.backup.manager.config;

import java.nio.file.Path;

@FunctionalInterface
public interface IFilter  { 
	public boolean test(Path p);

	default CombinedFilter or(IFilter other) {
		if(other instanceof CombinedFilter) {
			((CombinedFilter)other).add(this);
			return (CombinedFilter)other; 	
		}
		if(this instanceof CombinedFilter) {
			((CombinedFilter)this).add(other);
			return (CombinedFilter)this; 	
		}
		
		CombinedFilter c = new CombinedFilter();
		c.add(other);
		c.add(this);
		
		return c;
	}

}
