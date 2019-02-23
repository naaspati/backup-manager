package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.nopkg.SavedResource;

@Singleton
public class FileTreeManagerImpl implements FileTreeManager  { /* FIXME implements FileTreeFactory {
	 
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

	@Override
	public FileTree readFiletree(Config c, TreeType type, boolean createNewIfNotExists) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean saveFileTree(Config config) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean saveFileTree(FileTree fileTree) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean saveFileTree(Config c, FileTree fileTree) {
		// TODO Auto-generated method stub
		return false;
	}

}
