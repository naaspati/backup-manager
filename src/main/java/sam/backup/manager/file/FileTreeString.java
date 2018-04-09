package sam.backup.manager.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.function.Predicate;

import sam.myutils.MyUtils;
import se.sawano.java.text.AlphanumericComparator;

public class FileTreeString implements CharSequence {
	private final StringBuilder sb = new StringBuilder();
	private final Predicate<FileTreeEntity> filter;
	
	private static final AlphanumericComparator ALPHANUMERIC_COMPARATOR = new AlphanumericComparator();
	
	public FileTreeString(DirEntity dir, Predicate<FileTreeEntity> filter) {
		this.filter = filter;
		
		appendDetails(dir, new char[0]);
		walk(new char[] {' ', '|'}, dir);
	}
	public FileTreeString(DirEntity dir) {
		this(dir, (Predicate<FileTreeEntity>)null);
	}
	public FileTreeString(DirEntity dir, Collection<? extends FileTreeEntity> containsIn) {
		this(dir, filter(containsIn));
	}
	
	private static Predicate<FileTreeEntity> filter(Collection<? extends FileTreeEntity> containsIn) {
		if(containsIn.isEmpty())
			return (p -> false);
		
		IdentityHashMap<FileTreeEntity, Void> map = new IdentityHashMap<>();
		for (FileTreeEntity f : containsIn) {
			map.put(f, null);
			while((f = f.getParent()) != null) map.put(f, null);
		}
		return f -> {
			if(f instanceof FilteredDirEntity && map.containsKey(((FilteredDirEntity)f).getDir()))
				return true;
			return map.containsKey(f);
		};
	}
	private void walk(final char[] separator, DirEntity dir) {
		dir.sort((f1, f2) -> {
			boolean b1 = f1.isDirectory();
			boolean b2 = f2.isDirectory();

			if(b1 == b2)
				return ALPHANUMERIC_COMPARATOR.compare(f1.getfileNameString(), f2.getfileNameString());

			return b2 ? -1 : 1;
		});
		
		for (FileTreeEntity f : dir) {
			if(filter == null || filter.test(f)) {
				appendDetails(f, separator);
				
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

					walk(sp2, f.asDir());
				}
			}
		}
	}
	private void appendDetails(FileTreeEntity f, char[] separator) {
		sb.append(separator)
		.append(f.getfileNameString());

		
		AttrsKeeper ak = f.getSourceAttrs();
		long size = (ak.getCurrent() != null ? ak.getCurrent() : ak.getOld()).getSize();

		if(!f.isDirectory() ? size <= 0 : ( f.isDirectory() && f.asDir().isEmpty())) 
			sb.append('\n');
		else {
			sb.append('\t')
			.append('(');

			if(size > 0)
				MyUtils.bytesToHumanReadableUnits(size, false, sb);

			if(f.isDirectory() && !f.asDir().isEmpty())
				sb.append(' ').append(f.asDir().count()).append(" files");

			sb.append(")\n");	
		}
	}
	
	@Override
	public int length() {
		return sb.length();
	}
	@Override
	public char charAt(int index) {
		return sb.charAt(index);
	}
	@Override
	public CharSequence subSequence(int start, int end) {
		return sb.subSequence(start, end);
	}
	@Override
	public String toString() {
		return sb.toString();
	}

}
