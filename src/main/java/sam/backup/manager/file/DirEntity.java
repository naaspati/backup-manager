package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

import sam.backup.manager.file.FileTreeReader.Values;
import sam.backup.manager.walk.WalkMode;
import sam.myutils.MyUtils;
import se.sawano.java.text.AlphanumericComparator;

public class DirEntity extends FileTreeEntity  implements Iterable<FileTreeEntity>  {
	private final FTEArray children;
	private boolean walked;

	DirEntity(Path path, DirEntity parent) {
		super(path, parent);
		children = new FTEArray();
	}
	DirEntity(FileTreeReader reader, Values values, DirEntity parent) throws IOException {
		super(values, parent);

		if(!values.isDirectory())
			throw new IllegalArgumentException("not a dir: "+values);

		int size = values.size();
		children = new FTEArray(size);

		if(size <= 0)
			return;

		while(size-- > 0) {
			Values v = reader.next();
			FileTreeEntity f;
			if(v.isDirectory())
				f = new DirEntity(reader, v, this);
			else
				f = new FileEntity(v, this);

			children.add(f);
		} 
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

		for (int i = 0; i < children.size(); i++) {
			FileTreeEntity f = children.get(i);

			if(f.isDirectory() == isDir && f.getFileName().equals(fileName))
				return f;
		}

		FileTreeEntity f = isDir ? new DirEntity(fileName, this) : new FileEntity(fileName, this);
		children.add(f);
		return f;
	}
	FileVisitResult _walk(FileTreeWalker walker) {
		for (FileTreeEntity f : children) {
			if(f.isDirectory() && (((DirEntity)f).children.isEmpty()))
				continue;

			FileVisitResult result = f.isDirectory() ? walker.dir(f.castDir(), f.getSourceAttrs(), f.getBackupAttrs()) : walker.file(f.castFile(), f.getSourceAttrs(), f.getBackupAttrs());

			if(result == TERMINATE)
				return TERMINATE;

			if(result == SKIP_SIBLINGS || result == SKIP_SUBTREE)
				return CONTINUE;

			if(f.isDirectory() && f.castDir()._walk(walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}

	/**
	 * 	void updateDirModifiedTime() {
		if(getSourceAttrs() != null)
			setModifiedTime(getSourceAttrs().modifiedTime);

		for (FileTreeEntity f : getChildren()) {
			if(f.isDirectory())
				f.castDir().updateDirModifiedTime();
		}
	}
	 */

	private static final AlphanumericComparator ALPHANUMERIC_COMPARATOR = new AlphanumericComparator();

	void append(final char[] separator, final StringBuilder sb, Predicate<FileTreeEntity> filter) {
		if(children.isEmpty()) return;

		children.sort((f1, f2) -> {
			boolean b1 = f1.isDirectory();
			boolean b2 = f2.isDirectory();

			if(b1 == b2)
				return ALPHANUMERIC_COMPARATOR.compare(f1.getfileNameString(), f2.getfileNameString());

			return b2 ? -1 : 1;
		});

		children.stream()
		.filter(filter)
		.forEach(f -> {
			appendDetails(sb, f, separator);

			if(f.isDirectory()) {
				int length = separator.length;
				char[] sp2 = Arrays.copyOf(separator, length + 6);
				Arrays.fill(sp2, length, sp2.length, ' ');
				if(length != 2) {
					sp2[length - 1] = ' ';
					sp2[length - 2] = ' ';
				}
				sp2[sp2.length - 3] = '|';
				sp2[sp2.length - 2] = '_';
				sp2[sp2.length - 1] = '_';

				f.castDir().append(sp2, sb, filter);
			}
		});
	}
	static void appendDetails(StringBuilder sb, FileTreeEntity f, char[] separator) {
		sb.append(separator)
		.append(f.getfileNameString());

		long size = f.getSourceAttrs().getSize();

		if(!f.isDirectory() ? size <= 0 : ( f.isDirectory() && f.castDir().children.isEmpty())) 
			sb.append('\n');
		else {
			sb.append('\t')
			.append('(');

			if(size > 0)
				MyUtils.bytesToHumanReadableUnits(size, false, sb);

			if(f.isDirectory() && !f.castDir().children.isEmpty())
				sb.append(' ').append(f.castDir().children.size()).append(" files");

			sb.append(")\n");	
		}
	}
	public void computeSize(WalkMode w) {
		if(!walked)
			throw new IllegalStateException("dir not walked: "+this);

		if(children.isEmpty()) {
			atrk(w, this).setCurrentSize(0);
			return;
		}
		long size = 0;
		for (FileTreeEntity f : this) 
			size += size(w, f);			

		atrk(w, this).setCurrentSize(size);
	}
	private long size(WalkMode w, FileTreeEntity f) {
		Attrs atrs = atrk(w,f).getCurrent();
		
		if(atrs == null) {
			LOGGER.debug("attrs not found which calculating size: {}, {}",f, w);
			return 0;
		}
		return atrs.size;
	}
	private static AttrsKeeper atrk(WalkMode w, FileTreeEntity f) {
		return w == WalkMode.BACKUP ? f.getBackupAttrs() : f.getSourceAttrs();
	}
	
	@Override
	public void setCopied(boolean b) {
		super.setCopied(b);
		for (int i = 0; i < children.size(); i++)
			children.get(i).setCopied(b);
	}
	@Override
	public void setBackupable(boolean b, String reason) {
		super.setBackupable(b, reason);
		
		for (int i = 0; i < children.size(); i++)
			children.get(i).setBackupable(b, reason);
	}
	@Override
	public void setDeletable(boolean b) {
		super.setDeletable(b);
		
		for (int i = 0; i < children.size(); i++)
			children.get(i).setDeletable(b);
	}

	public boolean hasBackupable() {
		if(this.isBackupable())
			return true;
		
		for (FileTreeEntity f : children) {
			if(f.isDirectory() ? f.castDir().hasBackupable() : f.isBackupable())
				return true;
		}
		return false;
	}
	public boolean hasDeletable() {
		if(this.isDeletable())
			return true;
		for (FileTreeEntity f : children) {
			if(f.isDirectory() ? f.castDir().hasDeletable() : f.isDeletable())
				return true;
		}
		return false;
	}
	@Override
	public Iterator<FileTreeEntity> iterator() {
		return children.iterator();
	}
	public int size() {
		return children.size();
	}
	public boolean remove(FileTreeEntity ft) {
		return children.remove(ft);
	}
}
