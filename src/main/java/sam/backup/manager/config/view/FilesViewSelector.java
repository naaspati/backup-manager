package sam.backup.manager.config.view;

import sam.backup.manager.file.db.FileImpl;

public interface FilesViewSelector {
	public static FilesViewSelector backup() {
		return new FilesViewSelector() {
			@Override public void set(FileImpl ft, boolean value) { ft.getStatus().setBackupable(value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileImpl file) {return file.getStatus().isBackupable();}
		};
	};
	public static FilesViewSelector delete() {
		return new FilesViewSelector() {
			@Override public void set(FileImpl ft, boolean value) { ft.getStatus().setBackupDeletable(value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileImpl file) {return file.getStatus().isBackupDeletable(); }
		};
	}
	public static FilesViewSelector all() {
		return  new FilesViewSelector() {
			@Override public void set(FileImpl ft, boolean value) { }
			@Override public boolean isSelectable() { return false; }
			@Override public boolean get(FileImpl file) {return false;}
		};
	};
	
	public void set(FileImpl entity, boolean value);
	public boolean isSelectable();
	public boolean get(FileImpl entity);
}