package sam.backup.manager.file;

import java.nio.file.Path;
import java.util.function.Predicate;

import sam.backup.manager.walk.WalkMode;


public class FilteredFileTree extends FilteredDirEntity {
	private final FileTree ft;

	FilteredFileTree(FileTree ft, Predicate<FileTreeEntity> filter) {
		super(ft, null, filter);
		this.ft = ft;
		
		ft.computeSize(WalkMode.SOURCE);
		computeSize(WalkMode.SOURCE);
	}
	@Override
	public Path getBackupPath() {
		return ft.getBackupPath();
	}
}
