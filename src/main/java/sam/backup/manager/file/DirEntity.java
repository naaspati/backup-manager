package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import sam.backup.manager.file.FileTreeReader.Values;
import sam.backup.manager.walk.WalkMode;
import sam.myutils.myutils.MyUtils;
import se.sawano.java.text.AlphanumericComparator;

public class DirEntity extends FileTreeEntity  implements Iterable<FileTreeEntity>  {
	private final List<FileTreeEntity> children;
	private boolean walked;

	DirEntity(Path path, DirEntity parent) {
		super(path, parent);
		children = new ArrayList<>();
	}
	DirEntity(FileTreeReader reader, Values values, DirEntity parent) throws IOException {
		super(values, parent);

		if(!values.isDirectory())
			throw new IllegalArgumentException("not a dir: "+values);

		int size = values.getChildCount();
		children = new ArrayList<>(size);

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

	/**
	 * @Override
	void update(Path parent) {
		super.update(parent);

		if(children.isEmpty()) {
			setSize(0);
			return;
		}

		long size = 0;
		Path t = getTargetPath();
		Iterator<FileTreeEntity> itr = children.iterator();

		while (itr.hasNext()) {
			FileTreeEntity f = itr.next();
			if(walked && f.getSourceAttrs() == null && f.getBackupAttrs() == null) {
				itr.remove();
				System.out.println("removed from filetree: "+getfileNameString()+" -> "+f.getfileNameString());
			}
			else {
				f.update(t);
				size += f.getSize();
			}
		}
		setSize(size);
	}
	 * @param walker
	 * @return
	 */

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
		long size = children.stream()
				.mapToLong(f -> atrk(w, f).getCurrent().size)
				.sum();

		atrk(w, this).setCurrentSize(size);
	}
	private static AttrsKeeper atrk(WalkMode w, FileTreeEntity f) {
		return w == WalkMode.BACKUP ? f.getBackupAttrs() : f.getSourceAttrs();
	}

	private List<FileTreeEntity> backups;
	void backupNeeded(FileTreeEntity ft, boolean needed) {
		if(needed) {
			if(backups == null)
				backups = new ArrayList<>();
			if(backups.contains(ft))
				return;

			backups.add(ft);

			if(getParent() != null)
				getParent().backupNeeded(this, needed);
		} else if(backups != null && backups.remove(ft) && backups.isEmpty() && getParent() != null) {
			getParent().backupNeeded(this, false);
		}
	}
	@Override
	public boolean isBackupNeeded() {
		return backups != null && !backups.isEmpty();
	}
	@Override
	public boolean isDeleteFromBackup() {
		return children.stream().allMatch(FileTreeEntity::isDeleteFromBackup);
	}
	private boolean copied;
	void copied(FileTreeEntity ft) {
		copied = children.stream().filter(FileTreeEntity::isBackupNeeded).allMatch(FileTreeEntity::isCopied);

		if(children.isEmpty()) {
			if(getParent() != null)
				getParent().copied(this);
			super.setUpdated();
		}
	}
	@Override
	public boolean isCopied() {
		return copied;
	}
	void deleted(FileTreeEntity ft) {
		children.remove(ft);

		if(children.isEmpty() && getParent() != null)
			getParent().deleted(this);
	}

	@Override
	public Iterator<FileTreeEntity> iterator() {
		return children.iterator();
	}
	public int childCount() {
		return children.size();
	}
}
