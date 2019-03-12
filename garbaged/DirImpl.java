package sam.backup.manager.file;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.SIZED;

import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.backup.manager.file.api.FilteredDir;
import sam.collection.IntList;
import sam.collection.Iterators;
import sam.myutils.Checker;


public class DirImpl extends FileImpl implements Dir {
	public static final Predicate<FileEntity> ALWAYS_TRUE = e -> true;
	public static final FileEntity[] EMPTY_ARRAY = new FileEntity[0];

	protected int mod;
	protected final IntList children = new IntList();
	private long sourceSize = -1;
	
	protected DirImpl(int id, String filename, Attrs source, Attrs backup) {
		super(id, filename, source, backup);
	}
	protected DirImpl(int id, DirImpl parent, String filename, Attrs source, Attrs backup){
		super(id, parent, filename, source, backup);
	}
	private static FileEntity[] children(FileEntity[] children) {
		return Checker.isEmpty(children) ? EMPTY_ARRAY : children;
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
		return old.length + neew.size();
	}
	
	protected void modified() {
		parent.modified();
		mod++;
		sourceSize = -1;
	}
	public boolean isModified(int lastMod) {
		return lastMod != mod;
	}
	public int mod() {
		return mod;
	}
	FileEntity add(FileEntity f) {
		modified();

		if(neew.isEmpty())
			neew = new ArrayList<>();
		neew.add(f);
		return f;
	}

	@Override
	public void walk(FileTreeWalker walker) {
		walk(this, walker, ALWAYS_TRUE);
	}
	
	public FilteredDir filtered(Predicate<FileEntity> filter) {
		return new FilteredDirImpl(this, null, filter);
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
		
		for (FileEntity f : old) 
			sourceSize += f.getSourceSize();
		for (FileEntity f : neew) 
			sourceSize += f.getSourceSize();
		
		return sourceSize;
	}
	
	@Override
	public void forEach(Consumer<? super FileEntity> action) {
		Objects.requireNonNull(action);

		if(isEmpty())
			return;

		for (FileEntity f : old)
			action.accept(f);

		neew.forEach(action);
	}
	
	@Override
	public Iterator<FileEntity> iterator() {
		return iterator(this);
	}
	@Override
	public Spliterator<FileEntity> spliterator() {
		return spliterator(this);
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

	public static Spliterator<FileEntity> spliterator(DirImpl dir) {
		return spliterator(dir.old, dir.neew);
	}
	public static Iterator<FileEntity> iterator(DirImpl dir) {
		return iterator(dir.old, dir.neew);
	}
	public static Spliterator<FileEntity> spliterator(FileEntity[] old, Collection<FileEntity> neew) {
		int size = old.length + neew.size();
		
		if(size == 0)
			return Spliterators.emptySpliterator();
		
		return Spliterators.spliterator(iterator(old, neew), size, DISTINCT | IMMUTABLE | SIZED);
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
