package sam.backup.manager.file;

import java.util.Iterator;


public class DirImpl extends FileImpl implements Dir {
	private final FileTree filetree;
	
	protected DirImpl(int id, FileTree filetree, DirImpl parent, String filename, Attrs source, Attrs backup){
		super(id, parent, filename, source, backup);
		this.filetree  = filetree;
	}
	@Override
	public final boolean isDirectory() {
		return true;
	}
	@Override
	public int childrenCount() {
		return filetree.childrenCount(this);
	}
	@Override
	public Iterator<FileEntity> iterator() {
		return filetree.iterator(this);
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

