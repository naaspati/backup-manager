package sam.backup.manager.view.config;

import sam.backup.manager.file.FileEntity;

public interface FilesViewSelector {
	public static FilesViewSelector backup() {
		return new FilesViewSelector() {
			@Override public void set(FileEntity ft, boolean value) { ft.getStatus().setBackupable(value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileEntity file) {return file.getStatus().isBackupable();}
		};
	};
	public static FilesViewSelector delete() {
		return new FilesViewSelector() {
			@Override public void set(FileEntity ft, boolean value) { ft.getStatus().setBackupDeletable(value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileEntity file) {return file.getStatus().isBackupDeletable(); }
		};
	}
	public static FilesViewSelector all() {
		return  new FilesViewSelector() {
			@Override public void set(FileEntity ft, boolean value) { }
			@Override public boolean isSelectable() { return false; }
			@Override public boolean get(FileEntity file) {return false;}
		};
	};
	
	public void set(FileEntity entity, boolean value);
	public boolean isSelectable();
	public boolean get(FileEntity entity);
}