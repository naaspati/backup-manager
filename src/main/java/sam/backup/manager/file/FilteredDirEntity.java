package sam.backup.manager.file;

import java.util.function.Predicate;

public class FilteredDirEntity extends DirEntity {
	private final DirEntity dir;
	private final Predicate<FileTreeEntity> filter;

	FilteredDirEntity(DirEntity dir, FilteredDirEntity parent, Predicate<FileTreeEntity> filter) {
		super(dir, parent);

		dir.stream()
		.filter(filter)
		.map(f -> f.isDirectory() ? new FilteredDirEntity(f.asDir(), this, filter) : f)
		.filter(f -> f.isDirectory() ? !((FilteredDirEntity)f).isEmpty() : true)
		.forEach(this::add);

		this.dir = dir;
		this.filter = filter;
	}
	public Predicate<FileTreeEntity> getFilter() {
		return filter;
	}
	public DirEntity getDir() {
		return dir;
	}
	public boolean updateDirAttrs() {
		boolean b = true;
		for (FileTreeEntity f : this)
			b = (f.isDirectory() ? ((FilteredDirEntity)f).updateDirAttrs() : f.isCopied()) && b;
		
		if(b)
			markUpdated();
		
		return b;
	}
}
