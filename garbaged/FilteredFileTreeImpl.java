package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileEntityFilter;
import sam.backup.manager.file.api.FilteredFileTree;
import sam.backup.manager.walk.WalkMode;
import sam.myutils.ThrowException;

class FilteredFileTreeImpl extends FilteredDirImpl implements FilteredFileTree {

	public FilteredFileTreeImpl(FileTreeImpl me, FilteredDirImpl parent, Predicate<FileEntity> filter) {
		super(me, parent, filter);
	}

	@Override
	public FileEntity addFile(Path file, Attr af, WalkMode walkMode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dir addDir(Path dir, Attr attr, WalkMode walkMode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileEntity addFile(Dir parent, String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dir addDir(Dir parent, String dirname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWalked(Dir dir, boolean walked) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isWalked(Dir dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAttr(Attr attr, WalkMode walkMode, Path dir) {
		// TODO Auto-generated method stub

	}

	@Override
	public FilteredFileTree filtered(FileEntityFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(FileEntity f) {
		// TODO Auto-generated method stub
		return false;
	}

}
