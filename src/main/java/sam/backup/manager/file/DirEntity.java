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
import sam.myutils.myutils.MyUtils;

public class DirEntity extends FileTreeEntity {
	private final List<FileTreeEntity> children;

	DirEntity(Path path) {
		super(path);
		children = new ArrayList<>();
	}
	DirEntity(FileTreeReader reader, Values values) throws IOException {
		super(values);

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
				f = new DirEntity(reader, v);
			else
				f = new FileEntity(v);

			children.add(f);
		} 
	}
	public List<FileTreeEntity> getChildren() {
		return children;
	}
	public boolean isDeleteBackup() {
		return children.stream().anyMatch(FileTreeEntity::isDeleteBackup);
	}
	@Override
	public boolean isCopied() {
		return children.stream().allMatch(FileTreeEntity::isCopied);
	}
	@Override
	public boolean isBackupNeeded() {
		return children.stream().anyMatch(FileTreeEntity::isBackupNeeded);
	}
	@Override
	public boolean isDirectory() {
		return true;
	}

	FileTreeEntity addChild(Path fileName, boolean isDir) throws IOException {
		if(fileName.getNameCount() != 1)
			throw new IOException("bad filename expected length: 1; found length: "+fileName.getNameCount()+"  path: "+fileName);

		for (int i = 0; i < children.size(); i++) {
			FileTreeEntity f = children.get(i);

			if(f.isDirectory() == isDir && f.getFileName().equals(fileName))
				return f;
		}

		FileTreeEntity f = isDir ? new DirEntity(fileName) : new FileEntity(fileName);
		children.add(f);
		return f;
	}

	@Override
	void update(Path parent) {
		super.update(parent);

		if(children.isEmpty()) {
			setSize(0);
			return;
		}

		long size = 0;
		Path t = getTargetPath();
		for (FileTreeEntity f : children) {
			f.update(t);
			size += f.getSize();
		}
		setSize(size);
	}
	FileVisitResult _walk(FileTreeWalker walker) {
		for (FileTreeEntity f : children) {
			if(f.isDirectory() && (((DirEntity)f).getChildren().isEmpty()))
				continue;

			FileVisitResult result = f.isDirectory() ? walker.dir(f.castDir(), f.getSourceAboutFile(), f.getBackupAboutFile()) : walker.file(f.castFile(), f.getSourceAboutFile(), f.getBackupAboutFile());

			if(result == TERMINATE)
				return TERMINATE;

			if(result == SKIP_SIBLINGS || result == SKIP_SUBTREE)
				return CONTINUE;

			if(f.isDirectory() && f.castDir()._walk(walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}

	int cleanup() {
		Iterator<FileTreeEntity> ft = children.iterator();
		int sum = 0;
		while(ft.hasNext()) {
			FileTreeEntity f = ft.next();
			if(f.getSourceAboutFile() == null) {
				ft.remove();
				sum++;
			}
			else if(f.isDirectory())
				sum += f.castDir().cleanup();
		}
		return sum;
	}

	void updateDirModifiedTime() {
		if(getSourceAboutFile() != null)
			setModifiedTime(getSourceAboutFile().modifiedTime);

		for (FileTreeEntity f : getChildren()) {
			if(f.isDirectory())
				f.castDir().updateDirModifiedTime();
		}
	}
	void append(final char[] separator, final StringBuilder sb, Predicate<FileTreeEntity> filter) {
		if(children.isEmpty()) return;

		for (FileTreeEntity f : children) {
			if(!filter.test(f))
				continue;

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
		}
	}
	static void appendDetails(StringBuilder sb, FileTreeEntity f, char[] separator) {
		sb.append(separator)
		.append(f.getPathString());
		
		if(f.getSize() <= 0 && f.isDirectory() && f.castDir().getChildren().isEmpty()) {
			sb.append('\n');
			return;
		}
		
		sb.append('\t')
		.append('(');
		
		if(f.getSize() > 0)
			MyUtils.bytesToHumanReadableUnits(f.getSize(), false, sb);

		if(f.isDirectory() && !f.castDir().getChildren().isEmpty())
			sb.append(' ').append(f.castDir().getChildren().size()).append(" files");
		
		sb.append(")\n");
	}
}
