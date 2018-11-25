package sam.backup.manager.file.db;

import java.util.Iterator;
import java.util.List;


public class Dir extends FileImpl implements Iterable<FileImpl> {
	private final List<FileImpl> children;
	private final FileTree root;
	
	protected Dir(FileTree root, Dir parent, String filename, Attrs source, Attrs backup, List<FileImpl> children){
		super(parent, filename, source, backup);
		this.root = root;
		this.children = children; 
	}
	@Override
	public final boolean isDirectory() {
		return true;
	}
	public int childrenCount() {
		return children.size();
	}
	@Override
	public Iterator<FileImpl> iterator() {
		return children.iterator();
	}
	
	FileImpl addChild(FileImpl f) {
		if(f.getParent() != this)
			throw new IllegalArgumentException("i'm not your father");
		children.add(f);
		return f;
	}
	
	public FileImpl addFile(String filename) {
		return addChild(root.newFile(this, filename));
	}
	public Dir addDir(String filename) {
		return (Dir) addChild(root.newDir(this, filename));
	}
}

