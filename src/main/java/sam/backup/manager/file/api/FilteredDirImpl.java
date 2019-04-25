package sam.backup.manager.file.api;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

import sam.backup.manager.config.impl.PathWrap;
import sam.collection.Iterators;
import static sam.backup.manager.file.api.WithId.id;

public abstract class FilteredDirImpl implements FilteredDir, WithId {
	private final AbstractDir dir;
	private final FilteredDirImpl parent;
	private final Predicate<FileEntity> filter;
	private int childCount = -1;
	private int mod = -1;
	private long sourceSize = -1;

	public FilteredDirImpl(AbstractDir me, FilteredDirImpl parent, Predicate<FileEntity> filter) {
		super();
		this.dir = me;
		this.filter = filter;
		this.parent = parent;
		
		update();
	}
	private void update() {
		if(mod == mod(dir))
			return;
		
		childCount = 0;
		if(!dir.isEmpty()) {
			for (FileEntity f : dir) {
				if(filter.test(f))
					childCount++;
			}	
		}
		
		sourceSize = -1;
		mod = mod(dir);
	}

	protected abstract FilteredDir newFilteredDirImpl(AbstractDir dir, FilteredDirImpl filtered, Predicate<FileEntity> filter);
	protected abstract int mod(AbstractDir dir2);
	
    @Override 
	public int childrenCount() {
		update();
		return childCount;
	}
	
	/*
	 * u will see isEmpty() check everywhere, 
	 * it is to check if dir is modified since last access 
	 */
	@Override 
	public boolean isEmpty() {
		return childrenCount() == 0;
	}
	@Override 
	public int getId() {
		return id(dir);
	}
	@Override 
	public FilteredDir getParent() {
		return parent;
	}
	@Override 
	public Attrs getBackupAttrs() {
		return dir.getBackupAttrs();
	}
	@Override 
	public Attrs getSourceAttrs() {
		return dir.getSourceAttrs();
	}
	@Override 
	public void walk(FileTreeWalker walker) {
		AbstractDir.walk(dir, walker, filter);
	}
	@Override 
	public boolean isDirectory() {
		return true;
	}
	@Override 
	public Status getStatus() {
		return dir.getStatus();
	}
	@Override 
	public String getName() {
		return dir.getName();
	}
	@Override 
	public PathWrap getSourcePath() {
		return dir.getSourcePath();
	}
	@Override 
	public PathWrap getBackupPath() {
		return dir.getBackupPath();
	}
	@Override 
	public FilteredDir filtered(Predicate<FileEntity> filter0) {
		return newFilteredDirImpl(dir, this, filter.and(filter0));
	}
	
    @Override 
	public long getSourceSize() {
		if(isEmpty())
			return 0;
		
		if(sourceSize >= 0)
			return sourceSize;
		
		sourceSize = dir.computeSize(filter);
		return sourceSize;
	}

	@Override 
	public Iterator<FileEntity> iterator() {
		if(isEmpty())
			return Iterators.empty();
		else 
			return Iterators.filtered(dir.iterator(), filter);
	}
	@Override
	public Spliterator<FileEntity> spliterator() {
		if(isEmpty())
			return Spliterators.emptySpliterator();
		else 
			return Spliterators.spliteratorUnknownSize(iterator(), 0);
	}
	@Override
	public void forEach(Consumer<? super FileEntity> action) {
		if(isEmpty())
			return;
		
		for (FileEntity f : this) 
			action.accept(f);
	}
	@Override
	public int countFilesInTree() {
		return dir.countFilesInTree(filter);
	}
}
