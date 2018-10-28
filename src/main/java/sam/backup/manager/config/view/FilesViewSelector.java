package sam.backup.manager.config.view;

import sam.backup.manager.file.FileTreeEntity;

public interface FilesViewSelector {
	public static FilesViewSelector backup() {
		return new FilesViewSelector() {
			@Override public void set(FileTreeEntity ft, boolean value) { ft.setBackupable(value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileTreeEntity file) {return file.isBackupable();}
		};
	};
	public static FilesViewSelector delete() {
		return new FilesViewSelector() {
			@Override public void set(FileTreeEntity ft, boolean value) { ft.setBackupDeletable(value); }
			@Override public boolean isSelectable() { return true; }
			@Override public boolean get(FileTreeEntity file) {return file.isBackupDeletable(); }
		};
	}
	public static FilesViewSelector all() {
		return  new FilesViewSelector() {
			@Override public void set(FileTreeEntity ft, boolean value) { }
			@Override public boolean isSelectable() { return false; }
			@Override public boolean get(FileTreeEntity file) {return false;}
		};
	};
	
	public void set(FileTreeEntity entity, boolean value);
	public boolean isSelectable();
	public boolean get(FileTreeEntity entity);
}