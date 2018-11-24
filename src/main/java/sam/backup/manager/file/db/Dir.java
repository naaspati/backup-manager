package sam.backup.manager.file.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;


public class Dir extends FileImpl implements Iterable<FileImpl> {
	private static final FileImpl[] DEFAULT_ARRAY = {};
	
	private FileImpl[] children = DEFAULT_ARRAY;
	private List<FileImpl> list = Collections.emptyList();
	private final FileTree root;
	protected int modCount = 0;
	
	Dir(FileTree root, int id, Dir parent, String filename, Attrs source, Attrs backup){
		super(id, parent, filename, source, backup);
		this.root = root;
	}
	@Override
	public boolean isDirectory() {
		return true;
	}
	public int childrenCount() {
		return children.length + list.size();
	}
	protected void checkModified(int m) {
		if(m != modCount)
			throw new ConcurrentModificationException();
	}
	
	@Override
	public Iterator<FileImpl> iterator() {
		return new Iterator<FileImpl>() {
			int mod = modCount;
			int index = 0;
			int size = childrenCount();
			
			@Override
			public FileImpl next() {
				checkModified(mod);
				int n = index++;
				return n < children.length ? children[n] : list.get(n - children.length);
			}
			
			@Override
			public boolean hasNext() {
				return index < size;
			}
		};
	}
	
	private FileImpl addChild(FileImpl f) {
		if(list.isEmpty() && list.getClass() != ArrayList.class)
			list = new ArrayList<>();
		list.add(Objects.requireNonNull(f));
		return f;
	}
	
	public FileImpl addFile(String filename) {
		return addChild(root.newFile(filename));
	}
	public Dir addDir(String filename) {
		return (Dir) addChild(root.newDir(filename));
	}
	private void setChildren(FileImpl[] children) {
		this.children = children;
	}
	
}

