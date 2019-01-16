package sam.backup.manager.file;

import java.util.Iterator;
import java.util.List;

import sam.collection.Iterators;
import sam.myutils.Checker;


public class DirImpl extends FileImpl implements Dir {
	public static final FileEntity[] EMPTY_ARRAY = new FileEntity[0];
	
	private final FileTree filetree;
	private final FileEntity[] old;
	private List<FileEntity> neww;
	
	protected DirImpl(int id, FileTree filetree, DirImpl parent, String filename, Attrs source, Attrs backup, FileEntity[] children){
		super(id, parent, filename, source, backup);
		this.filetree  = filetree;
		this.old = children;
	}
	
	@Override
	public final boolean isDirectory() {
		return true;
	}
	@Override
	public boolean isEmpty() {
		return (old == EMPTY_ARRAY || Checker.isEmpty(old)) && Checker.isEmpty(neww);
	}
	@Override
	public int childrenCount() {
		return isEmpty() ? 0 : (old == null ? 0 : old.length) + (neww == null ? 0 : neww.size());
	}
	@Override
	public Iterator<FileEntity> iterator() {
		return Iterators.joi;
	}
	@Override
	public FileImpl addFile(String filename) {
		return (FileImpl)(filetree.addFile(this, filename));
	}
	@Override
	public DirImpl addDir(String filename) {
		return (DirImpl)filetree.addDir(this, filename);
	}
}

