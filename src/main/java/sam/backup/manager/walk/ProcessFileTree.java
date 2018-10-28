package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.List;

import sam.backup.manager.config.Config;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeWalker;

public class ProcessFileTree implements FileTreeWalker {
	private final boolean checkModified;
	private final boolean hardSync;
	private final boolean backupWalked;
	private final List<FileTreeEntity> willRemoved = new ArrayList<>();

	public ProcessFileTree(Config config, boolean backupWalked) {
		this.backupWalked = backupWalked;
		this.checkModified = config.getBackupConfig().checkModified();
		this.hardSync = config.getBackupConfig().hardSync();

		config.getFileTree().walk(this);
		willRemoved.forEach(FileTreeEntity::remove);
	}

	@Override
	public FileVisitResult file(FileEntity ft) {
		AttrsKeeper sourceK = ft.getSourceAttrs();
		Attrs source = sourceK.getCurrent();

		if(source == null) {
			if(hardSync)
				ft.setBackupDeletable(true);
			else
				willRemoved.add(ft);

			return CONTINUE;
		}

		isBackable(ft, true);

		return CONTINUE;
	}
	private void isBackable(FileTreeEntity ft, boolean isfile) {
		if(ft.getSourceAttrs().getCurrent() == null) 
			return;
		boolean backup = check(ft, isNew(ft), isfile ? "(1) new File" : "(3) new Directory");
		if(isfile)
			backup = backup || check(ft, checkModified && ft.getSourceAttrs().isModified(), "(2) File Modified");
	}

	private boolean check(FileTreeEntity f, boolean condition, String reason) {
		if(condition)
			f.setBackupable(true, reason);
		return condition;
	}

	@Override
	public FileVisitResult dir(DirEntity ft) {
		if(hardSyncCheck(ft))
			return SKIP_SUBTREE;

		if(!ft.isWalked())
			return SKIP_SUBTREE;

		isBackable(ft, false);
		return CONTINUE;
	}
	private boolean hardSyncCheck(FileTreeEntity ft) {
		boolean delete = hardSync && ft.getSourceAttrs().getCurrent() == null;
		ft.setBackupDeletable(delete);
		return delete;
	}
	private boolean isNew(FileTreeEntity ft) {
		if(ft.getSourceAttrs().getCurrent() == null) 
			return false;
		return backupWalked ? ft.getBackupAttrs().getCurrent() == null : ft.getSourceAttrs().getOld() == null;
	}
}
