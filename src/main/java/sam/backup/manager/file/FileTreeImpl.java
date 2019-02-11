package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Iterator;

import sam.backup.manager.extra.TreeType;
import sam.backup.manager.walk.WalkMode;
import sam.myutils.Checker;
import sam.nopkg.Junk;

final class FileTreeImpl implements FileTree {
	private final DirImpl me;
	private TreeType treetype;
	private final Path srcPath;
	private Path backupPath;
	private final BitSet dirWalked;

	FileTreeImpl(TreeType type, Path sourceDirPath, Path backupDirPath, int child_count) throws IOException {
		Checker.requireNonNull(
				"type sourceDirPath backupDirPath",  
				type,
				sourceDirPath,
				backupDirPath
				);

		this.me = Junk.notYetImplemented(); //FIXME
		this.treetype = type;
		this.srcPath = sourceDirPath;
		this.backupPath = backupDirPath;

		Junk.notYetImplemented();  // FIXME load filetree
		
		int MAX_DIR_ID = Junk.notYetImplemented(); //FIXME
		dirWalked = new BitSet(MAX_DIR_ID + 100);
	}
	
	FileTreeImpl(TreeType type, Path sourceDirPath, Path backupDirPath) throws IOException {
		this(type, sourceDirPath, backupDirPath, 0);
	}
	private String srcPathString, backupPathString;
	
	public String getSourcePath() { 
		return srcPathString != null ? srcPathString : ( srcPathString = srcPath.toString()); 
	}
	
	public String getBackupPath() {
		return backupPathString != null ? backupPathString : ( backupPathString = backupPath == null ? "" : backupPath.toString());
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
	public int childrenCount() {
		return me.childrenCount();
	}
	@Override
	public boolean isEmpty() {
		return me.isEmpty();
	}
	@Override
	public FileEntity addFile(String filename) {
		return me.addFile(filename);
	}
	@Override
	public Dir addDir(String dirname) {
		return me.addDir(dirname);
	}
	@Override
	public int getId() {
		return me.getId();
	}
	@Override
	public Dir getParent() {
		return null;
	}
	@Override
	public Attrs getBackupAttrs() {
		return me.getBackupAttrs();
	}
	@Override
	public Attrs getSourceAttrs() {
		return me.getSourceAttrs();
	}
	@Override
	public boolean isDirectory() {
		return true;
	}
	@Override
	public Status getStatus() {
		return me.getStatus();
	}
	@Override
	public String getName() {
		return me.getName();
	}
	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
	@Override
	public Iterator<FileEntity> iterator() {
		return me.iterator();
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
	public void walkCompleted() {
		//FIXME
				Junk.notYetImplemented();
	}

	@Override
	public void walkStarted(Path start) {
		// TODO Auto-generated method stub
		//FIXME
				Junk.notYetImplemented();
	}
}