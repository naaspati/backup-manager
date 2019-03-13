package sam.backup.manager.file;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.backup.manager.file.api.FilteredDir;
import sam.backup.manager.file.api.Status;
import sam.collection.Iterators;

class FilteredDirImpl implements FilteredDir, WithId {
	private final DirImpl dir;
	private final FilteredDirImpl parent;
	private final Predicate<FileEntity> filter;
	private int childCount = -1;
	private int mod = -1;
	private long sourceSize = -1;

	public FilteredDirImpl(DirImpl me, FilteredDirImpl parent, Predicate<FileEntity> filter) {
		super();
		this.dir = me;
		this.filter = filter;
		this.parent = parent;
		
		update();
	}
	private void update() {
		if(mod == dir.children().mod())
			return;
		
		childCount = 0;
		if(!dir.isEmpty()) {
			for (FileEntity f : dir) {
				if(filter.test(f))
					childCount++;
			}	
		}
		
		sourceSize = -1;
		mod = dir.children().mod();
	}

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
		return dir.getId();
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
		DirImpl.walk(dir, walker, filter);
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
		return new FilteredDirImpl(dir, this, filter.and(filter0));
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
