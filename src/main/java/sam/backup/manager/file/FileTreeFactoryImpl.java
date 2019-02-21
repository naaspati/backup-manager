package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.nio.file.FileVisitResult;

import javax.inject.Singleton;

import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTreeWalker;

@Singleton
public class FileTreeFactoryImpl  { /* FIXME implements FileTreeFactory {
	 
	 * public FileTree readFiletree(Config c, TreeType type, boolean createNewIfNotExists) throws Exception {
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
	 */
	
	//FIXME remove command after implementing FileTreeFactory @Override
	public void walk(Dir start, FileTreeWalker walker) {
		walk0(start, walker);
	}
	private FileVisitResult walk0(Dir start, FileTreeWalker walker) {
		for (FileEntity f : start) {
			if(f.isDirectory() && asDir(f).isEmpty())
				continue;

			FileVisitResult result = f.isDirectory() ? walker.dir(asDir(f)) : walker.file(f);

			if(result == TERMINATE)
				return TERMINATE;
			if(result == SKIP_SIBLINGS)
				break;

			if(result != SKIP_SUBTREE && f.isDirectory() && walk0(asDir(f), walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}
	private Dir asDir(FileEntity f) {
		return (Dir)f;
	}

}
