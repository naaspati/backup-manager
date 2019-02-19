package sam.backup.manager.file;

import java.util.Iterator;

import sam.backup.manager.config.PathWrap;

//FIXME move into DirImpl 
public class FilteredDirImpl implements FilteredDir {
	private final Dir dir;
	private final FilteredDirImpl parent;
	private final Predicate<FileEntity> filter;

	FilteredDirImpl(Dir dir, FilteredDirImpl parent, Predicate<FileEntity> filter) {
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
			b = (f.isDirectory() ? ((FilteredDirImpl)f).updateDirAttrs() : f.getStatus().isCopied()) && b;
		
		//FIXME if(b)  markUpdated();
		
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
					return new FilteredDirImpl((Dir)f, FilteredDirImpl.this, filter);
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
	@Override public PathWrap getSourcePath() { return dir.getSourcePath(); }
	@Override public PathWrap getBackupPath() { return dir.getBackupPath(); }
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public int childrenCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public FileEntity addFile(String filename) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Dir addDir(String dirname) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public long getSourceSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void walk(FileTreeWalker walker) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int filesInTree() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public FilteredDir filtered(Predicate<FileEntity> filter) {
		// TODO Auto-generated method stub
		return null;
	}
}
