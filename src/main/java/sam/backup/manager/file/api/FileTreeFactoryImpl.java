package sam.backup.manager.file.api;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.TreeType;
import sam.fx.alert.FxAlert;
import sam.nopkg.Junk;

public class FileTreeFactoryImpl implements FileTreeFactory {
	public FileTree readFiletree(Config c, TreeType type, boolean createNewIfNotExists) throws Exception {
		//FIXME 
		return Junk.notYetImplemented();
		// return FileTree.getInstance().newFileTree(c, type, createNewIfNotExists);
	}

	public boolean saveFileTree(Config config) {
		return Junk.notYetImplemented();
		//FIXME return saveFileTree(config, config.getFileTree());
	}

	public boolean saveFileTree(FileTree fileTree) {
		return saveFileTree(null, fileTree);
	}
	public boolean saveFileTree(Config c, FileTree fileTree) {
		try {
			fileTree.save();
			return true;
		} catch (Exception e) {
			FxAlert.showErrorDialog(c+"\n"+fileTree, "failed to save filetreee", e);
			return false;
		}
	}

}
