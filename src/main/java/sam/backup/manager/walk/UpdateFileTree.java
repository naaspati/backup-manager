package sam.backup.manager.walk;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.ContainsInFilter;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.FileTreeWalker;

public class UpdateFileTree implements FileTreeWalker {
	private final BackupNeeded backupNeeded = new BackupNeeded();
	private final boolean skipModifiedCheck;
	private final boolean backupWalked;
	private final List<FileTreeEntity> toRemove = new ArrayList<>();

	public UpdateFileTree(Config config, boolean skipModifiedCheck,
			boolean backupWalked) {
		this.skipModifiedCheck = skipModifiedCheck;
		this.backupWalked = backupWalked;

		config.getFileTree().walk(this);
		
		if(!toRemove.isEmpty())
			Utils.writeInTempDir("tree-clean-up", config.getSource(), ".txt", new FileTreeString(config.getFileTree(), new ContainsInFilter(toRemove)), LogManager.getLogger(getClass()));
	}
	
	@Override
	public FileVisitResult file(FileEntity ft) {
		AttrsKeeper sourceK = ft.getSourceAttrs();
		Attrs source = sourceK.getCurrent();

		backupNeeded.clear();
		
		if(source != null) {
			backupNeeded
			.test(isNew(ft), "(1) new File")
			.test(!skipModifiedCheck && sourceK.isModified(), "(2) File Modified");
		}

		if(backupNeeded.isNeeded()) 
			ft.setBackupable(backupNeeded.isNeeded(), backupNeeded.getReason());
		
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
		if(ft.getSourceAttrs().getCurrent() == null) {
			toRemove.add(ft);
			return false;
		}
		return backupWalked ? ft.getBackupAttrs().getCurrent() == null : ft.getSourceAttrs().getOld() == null;
	}
}
