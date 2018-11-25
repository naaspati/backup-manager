package sam.backup.manager.file.db;

import java.util.Iterator;
import java.util.List;


class DirImpl extends FileImpl implements Dir, ModifiableDir {
	private final List<FileEntity> children;
	private final FileTree2<FileImpl, DirImpl> root;
	
	protected DirImpl(FileTree2<FileImpl, DirImpl> root, DirImpl parent, String filename, Attrs source, Attrs backup, List<FileEntity> children){
		super(parent, filename, source, backup);
		this.root = root;
		this.children = children; 
	}
	@Override
	public final boolean isDirectory() {
		return true;
	}
	@Override
	public int childrenCount() {
		return children.size();
	}
	@Override
	public Iterator<FileEntity> iterator() {
		return children.iterator();
	}
	FileImpl addChild(FileImpl f) {
		if(f.getParent() != this)
			throw new IllegalArgumentException("i'm not your father");
		children.add(f);
		return f;
	}
	
	@Override
	public FileImpl addFile(String filename) {
		FileImpl f = root.newFile(this, filename);
		return addChild((FileImpl)f);
	}
	@Override
	public DirImpl addDir(String filename) {
		DirImpl dir = root.newDir(this, filename);
		addChild(dir);
		return dir;
	}
}

