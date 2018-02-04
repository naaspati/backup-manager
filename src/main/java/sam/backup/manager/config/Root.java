package sam.backup.manager.config;

import java.nio.file.Path;
import java.util.stream.Stream;

public class Root extends Base {
	private String backupRoot;
	private Config[] backups;
	private Config[] lists;

	private boolean backupsDisable;
	private boolean listsDisable;

	private transient Config[] _backups;
	private transient Config[] _lists;

	private transient Path fullBackupRoot;

	private Config[] _getBackups() {
		return backups == null || backups.length == 0 ? null : 
			_backups != null ? _backups : 
				(_backups = Stream.of(backups).filter(c -> !c.isDisabled()).toArray(Config[]::new)); 
	}
	private Config[] _getLists() {
		return lists == null || lists.length == 0 ? null : 
			_lists != null ? _lists : 
				(_lists = Stream.of(lists).filter(c -> !c.isDisabled()).toArray(Config[]::new)); 
	}

	@Override
	public boolean isModified() {
		return super.isModified() || (_getBackups() != null && Stream.of(_getBackups()).anyMatch(Config::isModified));
	}
	public Path getFullBackupRoot() {
		return fullBackupRoot;
	}
	public boolean hasLists() {
		return !listsDisable && _getLists() != null && _getLists().length != 0;
	}
	public Config[] getLists() {
		return _getLists();
	}
	public boolean hasBackups() {
		return  !backupsDisable && _getBackups() != null && _getBackups().length != 0;
	}
	public Config[] getBackups() {
		return _getBackups();
	}
	public void setDrive(Path drive) {
		fullBackupRoot = backupRoot == null ? drive : drive.resolve(backupRoot);
	}

	public void setRoot() {
		if(_getBackups() != null) {
			for (Config c : _getBackups())
				c.setRoot(this);
		}


	}
}
