package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.List;

import sam.backup.manager.config.Config;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.file.db.Attrs;
import sam.backup.manager.file.db.Dir;
import sam.backup.manager.file.db.FileEntity;

public class ProcessFileTree implements FileTreeWalker {
	private final boolean checkModified;
	private final boolean hardSync;
	private final boolean backupWalked;
	private final List<FileEntity> willRemoved = new ArrayList<>();

	public ProcessFileTree(Config config, boolean backupWalked) {
		this.backupWalked = backupWalked;
		this.checkModified = config.getBackupConfig().checkModified();
		this.hardSync = config.getBackupConfig().hardSync();

		config.getFileTree().walk(this);
		willRemoved.forEach(FileEntity::remove);
	}

	@Override
	public FileVisitResult file(FileEntity ft) {
		Attrs sourceK = ft.getSourceAttrs();
		Attr source = sourceK.current();

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
	private void isBackable(FileEntity ft, boolean isfile) {
		if(ft.getSourceAttrs().current() == null) 
			return;
		boolean backup = check(ft, isNew(ft), isfile ? "(1) new File" : "(3) new Directory");
		if(isfile)
			backup = backup || check(ft, checkModified && ft.getSourceAttrs().isModified(), "(2) File Modified");
	}

	private boolean check(FileEntity f, boolean condition, String reason) {
		if(condition)
			f.setBackupable(true, reason);
		return condition;
	}

	@Override
	public FileVisitResult dir(Dir ft) {
		if(hardSyncCheck(ft))
			return SKIP_SUBTREE;

		if(!ft.isWalked())
			return SKIP_SUBTREE;

		isBackable(ft, false);
		return CONTINUE;
	}
	private boolean hardSyncCheck(FileEntity ft) {
		boolean delete = hardSync && ft.getSourceAttrs().current() == null;
		ft.setBackupDeletable(delete);
		return delete;
	}
	private boolean isNew(FileEntity ft) {
		if(ft.getSourceAttrs().current() == null) 
			return false;
		return backupWalked ? ft.getBackupAttrs().current() == null : ft.getSourceAttrs().old() == null;
	}
}
