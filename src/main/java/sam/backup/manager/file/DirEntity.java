package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

import sam.backup.manager.walk.WalkMode;

public class DirEntity extends FileTreeEntity  implements Iterable<FileTreeEntity>  {
	private final FTEArray children;
	private boolean walked;
	
	public DirEntity(DirEntity e, DirEntity parent) {
		super(e, parent);
		this.walked = e.walked;
		children = new FTEArray(0);
	}
	DirEntity(Path path, DirEntity parent) {
		super(path, parent);
		children = new FTEArray();
	}
	
	public DirEntity(String fileNameString, DirEntity parent, Attrs sourceAttr, Attrs backupAttr) {
		super(fileNameString, parent, sourceAttr, backupAttr);
		this.children = new FTEArray(0);
	}
	protected void setChildren(FileTreeEntity[] children) {
		this.children.setAll(children);
	}
	@Override
	public boolean isDirectory() {
		return true;
	}
	public void setWalked(boolean walked) {
		this.walked = walked;
	}
	public boolean isWalked() {
		return walked;
	}
	FileTreeEntity addChild(Path fileName, boolean isDir) throws IOException {
		if(fileName.getNameCount() != 1)
			throw new IOException("bad filename expected length: 1; found length: "+fileName.getNameCount()+"  path: "+fileName);

		for (int i = 0; i < count(); i++) {
			FileTreeEntity f = children.get(i);

			if(f.isDirectory() == isDir && f.getFileName().equals(fileName))
				return f;
		}

		FileTreeEntity f = isDir ? new DirEntity(fileName, this) : new FileEntity(fileName, this);
		children.add(f);
		return f;
	}
	public FileVisitResult walk(FileTreeWalker walker) {
		for (FileTreeEntity f : children) {
			if(f.isDirectory() && f.asDir().isEmpty())
				continue;

			FileVisitResult result = f.isDirectory() ? walker.dir(f.asDir()) : walker.file(f.asFile());

			if(result == TERMINATE)
				return TERMINATE;
			if(result == SKIP_SIBLINGS)
				break;

			if(result != SKIP_SUBTREE && f.isDirectory() && f.asDir().walk(walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}
	
	long computeSize(WalkMode w) {
		if(!walked)
			return 0;
		
		if(isEmpty()) {
			atrk(w, this).setCurrentSize(0);
			return 0;
		}
		long size = 0;
		for (FileTreeEntity f : this) 
			size += f.isDirectory() ? f.asDir().computeSize(w) : size(atrk(w,f));			

		atrk(w, this).setCurrentSize(size);
		return size;
	}
	private long size(AttrsKeeper atrk) {
		return atrk.getCurrent() == null && atrk.getOld() == null ? 0 : atrk.getCurrent() == null ? atrk.getOld().size : atrk.getCurrent().size;
	}
	private static AttrsKeeper atrk(WalkMode w, FileTreeEntity f) {
		return w == WalkMode.BACKUP ? f.getBackupAttrs() : f.getSourceAttrs();
	}
	@Override
	public void setCopied(boolean b) {
		super.setCopied(b);
		forEach(f -> f.setCopied(b));
	}
	@Override
	public void setBackupable(boolean b) {
		super.setBackupable(b);
		for (FileTreeEntity f : children) 
			f.setBackupable(b);
	}
	@Override
	public void setBackupable(boolean b, String reason) {
		super.setBackupable(b, reason);
		for (FileTreeEntity f : children) 
			f.setBackupable(b, reason);
	}
	@Override
	public boolean isBackupable() {
		for (int i = 0; i < count(); i++) {
			if(children.get(i).isBackupable())
				return true;
		}
		return false;
	}
	public boolean isEmpty() {
		return count() == 0;
	}
	public int count() {
		return children.size();
	}
	@Override
	public Iterator<FileTreeEntity> iterator() {
		return children.iterator();
	}
	public boolean remove(FileTreeEntity ft) {
		return children.remove(ft);
	}
	public boolean hasDeletable() {
		throw new IllegalStateException("not yet implemented");
	}
	public Stream<FileTreeEntity> stream(){
		return children.stream();
	}
	public int filesInTree() {
		int size = 0;
		for (FileTreeEntity f : children) {
			if(f.isDirectory())
				size += f.asDir().filesInTree();
			else
				size++;
		}
		return size;
	}
	void sort(Comparator<FileTreeEntity> comparator) {
		children.sort(comparator);
	}
}

