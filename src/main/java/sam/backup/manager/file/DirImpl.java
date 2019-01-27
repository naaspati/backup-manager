package sam.backup.manager.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import sam.collection.Iterators;
import sam.myutils.Checker;


public class DirImpl extends FileImpl implements Dir {
	public static final FileEntity[] EMPTY_ARRAY = new FileEntity[0];
	
	private final FileEntity[] old;
	private List<FileEntity> neww = Collections.emptyList();
	private final Generator generator;
	
	protected DirImpl(Generator generator, int id, DirImpl parent, String filename, Attrs source, Attrs backup, FileEntity[] children){
		super(id, parent, filename, source, backup);
		this.old = Checker.isEmpty(children) ? EMPTY_ARRAY : children;
		this.generator = generator;
	}
	
	@Override
	public final boolean isDirectory() {
		return true;
	}
	@Override
	public boolean isEmpty() {
		return childrenCount() == 0;
	}
	@Override
	public int childrenCount() {
		return old.length + neww.size();
	}
	@Override
	public Iterator<FileEntity> iterator() {
		if(isEmpty())
			return Iterators.empty();
		return Iterators.join(Iterators.of(old), neww.iterator());
	}
	@Override
	public FileImpl addFile(String filename) {
		return (FileImpl)(add(generator.newFile(filename)));
	}
	@Override
	public DirImpl addDir(String dirname) {
		return (DirImpl)add(generator.newDir(dirname));
	}

	private FileEntity add(FileEntity f) {
		if(Checker.isEmpty(neww))
			neww = new ArrayList<>();
		neww.add(f);
		return f;
	}
}

