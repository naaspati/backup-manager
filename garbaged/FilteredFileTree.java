package sam.backup.manager.file.api;

import java.io.IOException;
import java.nio.file.Path;

import sam.myutils.ThrowException;

public interface FilteredFileTree extends FileTree, FilteredDir {
	
	/**
	 *  access not allowed
	 */
	@Deprecated
	@Override
	default void save() throws IOException {
		ThrowException.illegalAccessError();
	}

	/**
	 *  access not allowed
	 */
	@Override
	default void forcedMarkUpdated() {
		ThrowException.illegalAccessError();
	}

	/**
	 *  access not allowed
	 */
	@Override
	default void walkCompleted() {
		ThrowException.illegalAccessError();
	}

	/**
	 *  access not allowed
	 */
	@Override
	default void walkStarted(Path start) {
		ThrowException.illegalAccessError();
	}
}
