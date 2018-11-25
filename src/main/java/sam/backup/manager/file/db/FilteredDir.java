package sam.backup.manager.file.db;

import java.util.Iterator;
import java.util.function.Predicate;

public class FilteredDir implements Dir {
	private final Dir dir;
	private final FilteredDir parent;
	private final Predicate<FileEntity> filter;

	FilteredDir(Dir dir, FilteredDir parent, Predicate<FileEntity> filter) {
		this.dir = dir;
		this.filter = filter;
		this.parent = parent;
	}
	public Predicate<FileEntity> getFilter() {
		return filter;
	}
	public Dir getDir() {
		return dir;
	}
	public boolean updateDirAttrs() {
		boolean b = true;
		for (FileEntity f : this)
			b = (f.isDirectory() ? ((FilteredDir)f).updateDirAttrs() : f.isCopied()) && b;
		
		if(b)
			markUpdated();
		
		return b;
	}
	@Override
	public Iterator<FileEntity> iterator() {
		Iterator<FileEntity> itr = dir.iterator();
		return new Iterator<FileEntity>() {
			FileEntity next = null;
			{
				next0();
			}
			private void next0() {
				next = null;
				while(itr.hasNext()){
					FileEntity f = itr.next();
					if(filter.test(f)) {
						next = f;
						break;
					}
				}
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}
			@Override
			public FileEntity next() {
				FileEntity f = next;
				next0();
				if(f.isDirectory())
					return new FilteredDir((Dir)f, FilteredDir.this, filter);
				return f;
			}
		};
	}
	
	@Override public Dir getParent() { return parent; }
	@Override public Attrs getBackupAttrs() { return dir.getBackupAttrs(); }
	@Override public Attrs getSourceAttrs() { return dir.getSourceAttrs(); }
	@Override public boolean isDirectory() { return true; }
	@Override public Status getStatus() { return dir.getStatus(); }
	@Override public String getName() { return dir.getName(); }
	@Override public String getSourcePath() { return dir.getSourcePath(); }
	@Override public String getBackupPath() { return dir.getBackupPath(); }
}
