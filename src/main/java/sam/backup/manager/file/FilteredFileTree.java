package sam.backup.manager.file;

import java.nio.file.Path;
import java.util.function.Predicate;

import sam.backup.manager.walk.WalkMode;


public class FilteredFileTree extends FilteredDirEntity {
	private final FileTree ft;

	public FilteredFileTree(FileTree ft, WalkMode mode, Predicate<FileTreeEntity> filter) {
		super(ft, null, filter);
		this.ft = ft;
		
		// FIXME
	//	ft.computeSize(mode);
	//	computeSize(mode);
	}
	@Override
	public Path getBackupPath() {
		return ft.getBackupPath();
	}
}
