package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;

import sam.backup.manager.config.PathWrap;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeEditor;
import sam.backup.manager.walk.WalkMode;
import sam.myutils.Checker;
import sam.nopkg.Junk;

final class FileTreeImpl extends DirImpl implements FileTree {
	private TreeType treetype;
	private final PathWrap srcPath;
	private PathWrap backupPath;
	private final BitSet dirWalked;
	private final Serial dirSerial = new Serial(1);
	private final Serial fileSerial = new Serial(0);

	FileTreeImpl(TreeType type, Path sourceDirPath, Path backupDirPath, Attrs source, Attrs backup, int children_count) throws IOException {
		super(0, sourceDirPath.toString(), source, backup, new FileEntity[children_count]);
		Checker.requireNonNull("type sourceDirPath backupDirPath", type, sourceDirPath,backupDirPath);

		this.treetype = type;
		this.srcPath = new PathWrap(sourceDirPath);
		this.backupPath = new PathWrap(backupDirPath);

		Junk.notYetImplemented();  // FIXME load filetree
		
		int MAX_DIR_ID = Junk.notYetImplemented(); //FIXME
		dirWalked = new BitSet(MAX_DIR_ID + 100);
	}
	
	@Override
	public FileTreeEditor getEditor(Path start) {
		return new FileTreeEditor() {
			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub
				
			}
			@Override
			public boolean delete(FileEntity f) {
				// TODO Auto-generated method stub
				return Junk.notYetImplemented();
			}
			@Override
			public Dir addDir(Path dir, Attr attr, WalkMode walkMode) {
				//FIXME
				return Junk.notYetImplemented();
			}
			@Override
			public void setAttr(Attr attr, WalkMode walkMode, Path dir) {
				//FIXME
						Junk.notYetImplemented();
			}
			@Override
			public FileEntity addFile(Path file, Attr af, WalkMode walkMode) {
				//FIXME
						return Junk.notYetImplemented();
			}
			@Override
			public void setWalked(Dir dir, boolean walked) {
				dirWalked.set(dir.getId(), walked);
			}
			@Override
			public boolean isWalked(Dir dir) {
				return dirWalked.get(dir.getId());
			}
			
			@Override
			public FileImpl addFile(Dir parent, String filename) {
				return add(parent, new FileImpl(fileSerial.next(), (DirImpl)parent, filename, EMPTY_ATTRS, EMPTY_ATTRS));
			}
			@SuppressWarnings("unchecked")
			private <E extends FileImpl> E add(Dir parent, E file) {
				return (E) ((DirImpl)parent).add(file);
			}
			@Override
			public DirImpl addDir(Dir parent, String dirname) {
				return add(parent, new DirImpl(dirSerial.next(), (DirImpl)parent, dirname, EMPTY_ATTRS, EMPTY_ATTRS, EMPTY_ARRAY));
			}
		};
	}
	
	@Override
	public boolean isWalked(Dir dir) {
		return dirWalked.get(dir.getId());
	}
	@Override
	protected void modified() {
		mod++;
		//TODO notify listener;
	}
	
	@Override
	public PathWrap getSourcePath() { 
		return srcPath; 
	}
	@Override
	public PathWrap getBackupPath() {
		return backupPath;
	}
	public TreeType getTreetype(){ return this.treetype; }

	public void forcedMarkUpdated() {
		//TODO serializer.applyToAll(f -> f.getSourceAttrs().setUpdated());
		Junk.notYetImplemented();
	}
	public void save() throws IOException {
		//TODO serializer.save();
		Junk.notYetImplemented();
		
	}
	@Override
	public Dir getParent() {
		return null;
	}
}