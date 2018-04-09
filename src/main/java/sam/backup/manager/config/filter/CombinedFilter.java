package sam.backup.manager.config.filter;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class CombinedFilter implements IFilter, HasFilterArrays {
	IFilter[] filters = new IFilter[0]; 

	@Override
	public boolean test(Path p) {
		for (IFilter f : filters) 
			if(f.test(p))
				return true;
		return false;
	}

	void add(IFilter f) {
		filters = Arrays.copyOf(filters, filters.length + 1);
		filters[filters.length - 1] = f;
	}

	@Override
	public Map<String, String[]> getArrays() {
		return Arrays.stream(filters)
				.filter(c -> c instanceof HasFilterArrays)
				.map(c -> (HasFilterArrays)c)
				.map(HasFilterArrays::getArrays)
				.flatMap(m -> m.entrySet().stream())
				.collect(groupingBy(Entry::getKey, mapping(Entry::getValue, Collectors.reducing(new String[0], this::combine))));
	}
	
	private String[] combine(String[] a1, String[] a2) {
		if(a1 == null && a2 == null)
			return null;
		if(a1 == null)
			return a2;
		if(a2 == null)
			return a1;
		
		String[] array = new String[a1.length + a2.length];
		int n = 0;
		for (String s : a1) array[n++] = s;
		for (String s : a2) array[n++] = s;
		
		return array;
	}
	@Override
	public String toString() {
		return asString();
	}
}
