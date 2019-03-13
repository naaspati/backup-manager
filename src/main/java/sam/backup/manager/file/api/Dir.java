package sam.backup.manager.file.api;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate; 

public interface  Dir extends FileEntity, Iterable<FileEntity> {
	int childrenCount();
	default boolean isEmpty() {
		return childrenCount() == 0;
	}
	public void walk(FileTreeWalker walker);
	FilteredDir filtered(Predicate<FileEntity> filter);
	int countFilesInTree();
	
	@Override
	default void forEach(Consumer<? super FileEntity> action) {
		if(isEmpty())
			return;

		Objects.requireNonNull(action);

		for (FileEntity f : this)
			action.accept(f);
	}
	@Override
	default Spliterator<FileEntity> spliterator() {
		if(isEmpty())
			return Spliterators.emptySpliterator();
		
		return Spliterators.spliterator(iterator(), childrenCount(), Spliterator.ORDERED);
	}
}

