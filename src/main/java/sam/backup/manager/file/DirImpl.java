package sam.backup.manager.file;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.nio.file.FileVisitResult;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.backup.manager.file.api.FilteredDir;
import sam.collection.IntSet;
import sam.collection.Iterators;
import sam.myutils.Checker;


public class DirImpl extends FileImpl implements Dir {
	public static final Predicate<FileEntity> ALWAYS_TRUE = e -> true;
	public static final FileEntity[] EMPTY_ARRAY = new FileEntity[0];
	
	private final DirHelper dirHelper;
	private final IntSet children;
	private int mod;
	
	private long sourceSize = -1;
	
	public DirImpl(int id, String filename, DirImpl parent, FileHelper fileHelper, DirHelper dirHelper, int childCount) {
		super(id, filename, parent, fileHelper);
		this.dirHelper = dirHelper;
		this.children = new IntSet(childCount + 5);
	}
	public void add(int id) {
		children.add(id);
	}
	
	@Override
	public final boolean isDirectory() {
		return true;
	}
	@Override
	public void walk(FileTreeWalker walker) {
		walk(this, walker, ALWAYS_TRUE);
	}
	
	public FilteredDir filtered(Predicate<FileEntity> filter) {
		return new FilteredDirImpl(this, null, filter);
	}

	@Override
	public int childrenCount() {
		return children.size();
	}
	@Override
	public Iterator<FileEntity> iterator() {
		if(children.isEmpty())
			return Collections.emptyIterator();
		int modm = this.mod;
		
		return new Iterator<FileEntity>() {
			int n = 0;
			
			@Override
			public FileEntity next() {
				if(modm != mod)
					throw new ConcurrentModificationException();
				if(n >= children.size())
					throw new NoSuchElementException();
				
				return dirHelper().file(children.get(n++));
			}
			@Override
			public boolean hasNext() {
				return n < children.size();
			}
		};
	}
	
	protected DirHelper dirHelper() {
		return dirHelper;
	}
	
	@Override
	public long getSourceSize() {
		if(isEmpty())
			return 0;
		
		if(sourceSize != -1)
			return sourceSize;
		
		sourceSize = 0;
		if(isEmpty())
			return sourceSize;
		
		for (FileEntity f : this) 
			sourceSize += f.getSourceSize();
		
		return sourceSize;
	}
	
	@Override
	public void forEach(Consumer<? super FileEntity> action) {
		if(isEmpty())
			return;
		
		Objects.requireNonNull(action);

		for (FileEntity f : this)
			action.accept(f);
	}
	
	public static FileVisitResult walk(DirImpl dir, FileTreeWalker walker, Predicate<FileEntity> filter) {
		if(dir.isEmpty())
			return CONTINUE;
		
		for (FileEntity f : dir) {
			if(!filter.test(f))
				continue;

			FileVisitResult result = walkApply(f, walker, filter);

			if(result == TERMINATE)
				return TERMINATE;
			if(result == SKIP_SIBLINGS)
				break;
		}
		
		return CONTINUE;
	}

	private static FileVisitResult walkApply(FileEntity f, FileTreeWalker walker, Predicate<FileEntity> filter) {
		if(!filter.test(f) || (f.isDirectory() && dir(f).isEmpty()))
			return CONTINUE;


		FileVisitResult result = f.isDirectory() ? walker.dir(dir(f)) : walker.file(f);

		if(result == TERMINATE || result == SKIP_SIBLINGS)
			return result;

		if(result != SKIP_SUBTREE && f.isDirectory() && walk(dir(f), walker, filter) == TERMINATE)
			return TERMINATE;

		return CONTINUE;
	}

	static DirImpl dir(FileEntity f) {
		return (DirImpl)f;
	}
	
	public static Iterator<FileEntity> iterator(FileEntity[] old, Collection<FileEntity> neew) {
		boolean o = Checker.isEmpty(old);
		boolean n = Checker.isEmpty(neew);
		
		if(o && n)
			return Iterators.empty();
		else if(o || n)
			return (n ? Iterators.of(old) : neew.iterator());
		else
			return Iterators.join(Iterators.of(old), neew.iterator());
	}
	@Override
	public int countFilesInTree() {
		return countFilesInTree(ALWAYS_TRUE);	
	}
	public int countFilesInTree(Predicate<FileEntity> filter) {
		if(isEmpty())
			return 0;
		
		int n = 0;
		
		for (FileEntity f : this) {
			if(!filter.test(f))
				continue;
				
			if(f.isDirectory())
				n += ((Dir)f).countFilesInTree();
			else 
				n++;
		}
		return n;
	}
}

