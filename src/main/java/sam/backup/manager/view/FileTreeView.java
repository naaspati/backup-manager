package sam.backup.manager.view;

import javafx.scene.control.TreeItem;
import sam.backup.manager.file.FileTree;

public class FileTreeView extends TreeItem<FileTree> {
	private final FileTree ft;

	public FileTreeView(FileTree ft, boolean onlyExistsCheck) {
		super(ft);
		this.ft = ft;
		
		if(ft.getChildren() != null && !ft.getChildren().isEmpty()) {
			ft.getChildren().stream()
			.filter(f -> f.backupNeeded(onlyExistsCheck))
			.map(f -> new FileTreeView(f, onlyExistsCheck))
			.filter(v -> !v.getFileTree().isDirectory() || !v.getChildren().isEmpty())
			.forEach(getChildren()::add);
		}
	}
	public FileTree getFileTree() {
		return ft;
	}
}
