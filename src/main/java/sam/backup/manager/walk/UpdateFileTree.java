package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javafx.application.Platform;
import javafx.stage.Stage;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.view.FilesView;
import sam.backup.manager.config.view.FilesViewMode;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.ContainsInFilter;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.file.FilteredDirEntity;
import sam.backup.manager.file.FilteredFileTree;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.myutils.MyUtils;

public class UpdateFileTree implements FileTreeWalker {
	private final BackupNeeded backupNeeded = new BackupNeeded();
	private final boolean skipModifiedCheck;
	private final boolean backupWalked;
	private final boolean backupWalkedDisabled;
	private final List<FileTreeEntity> toRemove = new ArrayList<>();

	public UpdateFileTree(Config config, boolean skipModifiedCheck,
			boolean backupWalked) {
		this.skipModifiedCheck = skipModifiedCheck;
		this.backupWalked = backupWalked;

		backupWalkedDisabled = config.isNoBackupWalk();
		config.getFileTree().walk(this);
		
		if(!toRemove.isEmpty())
			Platform.runLater(() -> cleaner(config));
	}

	private void cleaner(Config config) {
		FilteredFileTree tree = new FilteredFileTree(config.getFileTree(), new ContainsInFilter(toRemove));
		IdentityHashMap<FileTreeEntity, Boolean> map = new IdentityHashMap<>();
		toRemove.forEach(s -> map.put(s, true));
		
		CustomButton ok = new CustomButton(ButtonType.OK);
		CustomButton cancel = new CustomButton(ButtonType.CANCEL);
		
		Stage stage = FilesView.open(config, tree, new FilesViewMode() {
			@Override public void set(FileTreeEntity file, boolean value) { map.put(file, value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileTreeEntity file) { return MyUtils.nullSafe(map.get(file), true); }
		});
		
		ok.setEventHandler(btn -> {
			map.forEach((s,t) -> {
				if(t) {
					if(s instanceof FileEntity)
						s.remove();
					else if(s instanceof FilteredDirEntity) 
						((FilteredDirEntity)s).getDir().remove();
				}
			});
			
			map.keySet().stream()
			.map(f -> f instanceof FilteredDirEntity ? ((FilteredDirEntity)f).getDir() : f.getParent())
			.distinct()
			.filter(DirEntity::isEmpty)
			.forEach(FileTreeEntity::remove);
		});
		cancel.setEventHandler(btn -> stage.hide());
	}

	@Override
	public FileVisitResult file(FileEntity ft) {
		AttrsKeeper sourceK = ft.getSourceAttrs();
		Attrs source = sourceK.getCurrent();

		if(source != null) {
			backupNeeded
			.test(isNew(ft), "(1) new File")
			.test(!skipModifiedCheck && sourceK.isModified(), "(2) File Modified");
		}

		if(backupNeeded.isNeeded()) {
			ft.setBackupable(backupNeeded.isNeeded(), backupNeeded.getReason());
		}
		return CONTINUE;
	}
	@Override
	public FileVisitResult dir(DirEntity ft) {
		if(!ft.isWalked())
			return SKIP_SUBTREE;

		if(isNew(ft)) {
			ft.setBackupable(true, "(3) new File/Directory");
			return SKIP_SUBTREE;	
		}
		return CONTINUE;
	}
	private boolean isNew(FileTreeEntity ft) {
		//check non-existent of file
		boolean b = backupWalked && ft.getBackupAttrs().getCurrent() == null && ft.getSourceAttrs().getCurrent() == null;
		b = b || backupWalkedDisabled && ft.getSourceAttrs().getOld() == null; 
		
		if(b) {
			toRemove.add(ft);
			return false;
		}
		return backupWalked ? ft.getBackupAttrs().getCurrent() == null : ft.getSourceAttrs().getOld() == null;
	}
}
