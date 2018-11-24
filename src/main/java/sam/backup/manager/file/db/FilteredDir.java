package sam.backup.manager.file.db;

import java.util.function.Predicate;

public class FilteredDir extends Dir {
	private final Dir dir;
	private final Predicate<FileImpl> filter;

	FilteredDir(Dir dir, FilteredDir parent, Predicate<FileImpl> filter) {
		//TODO
		super(null, null);

		dir.stream()
		.filter(filter)
		.map(f -> f.isDirectory() ? new FilteredDirImpl(asDir(f), this, filter) : f)
		.filter(f -> f.isDirectory() ? !((FilteredDirImpl)f).isEmpty() : true)
		.forEach(this::add);

		this.dir = dir;
		this.filter = filter;
	}
	public Predicate<FileImpl> getFilter() {
		return filter;
	}
	public Dir getDir() {
		return dir;
	}
	public boolean updateDirAttrs() {
		boolean b = true;
		for (FileImpl f : this)
			b = (f.isDirectory() ? ((FilteredDirImpl)f).updateDirAttrs() : f.isCopied()) && b;
		
		if(b)
			markUpdated();
		
		return b;
	}
}
