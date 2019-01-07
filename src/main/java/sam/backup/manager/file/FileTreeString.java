package sam.backup.manager.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import sam.myutils.MyUtilsBytes;

public class FileTreeString implements CharSequence {
	private final StringBuilder sb = new StringBuilder();
	private final Predicate<FileEntity> filter;
	
	public FileTreeString(Dir dir, Predicate<FileEntity> filter) {
		this.filter = filter;
		
		appendDetails(dir, new char[0]);
		walk(new char[] {' ', '|'}, dir);
	}
	public FileTreeString(Dir dir) {
		this(dir, (Predicate<FileEntity>)null);
	}
	public FileTreeString(Dir dir, Collection<? extends FileEntity> containsIn) {
		this(dir, new ContainsInFilter(containsIn));
	}
	
	//TODO private static final AlphanumericComparator ALPHANUMERIC_COMPARATOR = new AlphanumericComparator();
	
	private void walk(final char[] separator, Dir dir) {
		
		/**
		 * TODO  i dont think sorting is that much required
		 * dir.sort((f1, f2) -> {
			boolean b1 = f1.isDirectory();
			boolean b2 = f2.isDirectory();

			if(b1 == b2)
				return ALPHANUMERIC_COMPARATOR.compare(f1.getName(), f2.getName());

			return b2 ? -1 : 1;
		});
		 */
		
		for (FileEntity f : dir) {
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

					walk(sp2, asDir(f));
				}
			}
		}
	}
	private Dir asDir(FileEntity f) {
		return (Dir)f;
	}
	private void appendDetails(FileEntity f, char[] separator) {
		sb.append(separator)
		.append(f.getName());
		
		Attrs ak = f.getSourceAttrs();
		long size = ak.size();

		if(!f.isDirectory() ? size <= 0 : ( f.isDirectory() && asDir(f).childrenCount() == 0)) 
			sb.append('\n');
		else {
			sb.append('\t')
			.append('(');

			if(size > 0)
				MyUtilsBytes.bytesToHumanReadableUnits(size, false, sb);

			if(f.isDirectory() && asDir(f).childrenCount() != 0)
				sb.append(' ').append(asDir(f).childrenCount()).append(" files");

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
