package sam.backup.manager.file;

import java.util.function.Predicate;

import sam.backup.manager.file.db.DirImpl;
import sam.backup.manager.file.db.FileImpl;

public class FilteredDirEntity extends DirImpl {
	private final DirImpl dir;
	private final Predicate<FileImpl> filter;

	FilteredDirEntity(DirImpl dir, FilteredDirEntity parent, Predicate<FileImpl> filter) {
		//TODO
		super(null, null);

		dir.stream()
		.filter(filter)
		.map(f -> f.isDirectory() ? new FilteredDirImpl(f.asDir(), this, filter) : f)
		.filter(f -> f.isDirectory() ? !((FilteredDirImpl)f).isEmpty() : true)
		.forEach(this::add);

		this.dir = dir;
		this.filter = filter;
	}
	public Predicate<FileImpl> getFilter() {
		return filter;
	}
	public DirImpl getDir() {
		return dir;
	}
	public boolean updateDirAttrs() {
		boolean b = true;
		for (FileTreeEntity f : this)
			b = (f.isDirectory() ? ((FilteredDirImpl)f).updateDirAttrs() : f.isCopied()) && b;
		
		if(b)
			markUpdated();
		
		return b;
	}
}
