package sam.backup.manager.file.api;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static sam.backup.manager.file.api.FilteredDir.ALWAYS_TRUE;

import java.nio.file.FileVisitResult;
import java.util.function.Predicate;
public abstract class AbstractDirImpl extends AbstractFileImpl implements Dir {
	public static final FileEntity[] EMPTY_ARRAY = new FileEntity[0];

	private long sourceSize = -1;

	public AbstractDirImpl(String filename) {
		super(filename);
	}

	@Override
	public final boolean isDirectory() {
		return true;
	}
	@Override
	public void walk(FileTreeWalker walker) {
		walk(this, walker, ALWAYS_TRUE);
	}

	@Override
	public long getSourceSize() {
		if(isEmpty())
			return 0;

		if(sourceSize != -1)
			return sourceSize;

		sourceSize = computeSize(ALWAYS_TRUE);
		return sourceSize;
	}

	public static FileVisitResult walk(Dir dir, FileTreeWalker walker, Predicate<FileEntity> filter) {
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

	static AbstractDirImpl dir(FileEntity f) {
		return (AbstractDirImpl)f;
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
				n += dir(f).countFilesInTree();
			else 
				n++;
		}
		return n;
	}

	public long computeSize(Predicate<FileEntity> filter) {
		if(isEmpty())
			return 0;

		long sourceSize = 0;
		for (FileEntity f : this) {
			if(filter.test(f)) {
				if(f.isDirectory()) {
					sourceSize += dir(f).computeSize(filter);
				} else {
					sourceSize += f.getSourceSize();
				} 
			}
		}
		return  sourceSize;
	}
}

