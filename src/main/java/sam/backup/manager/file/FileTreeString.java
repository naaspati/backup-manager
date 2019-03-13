package sam.backup.manager.file;

import static sam.backup.manager.file.FileUtils.ALWAYS_TRUE;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import sam.backup.manager.Utils;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.myutils.MyUtilsBytes;

class FileTreeString {
	private final Predicate<FileEntity> filter;
	private final Dir dir;
	
	public FileTreeString(Dir dir, Predicate<FileEntity> filter) {
		this.filter = filter == null ? ALWAYS_TRUE : filter;
		this.dir = dir;
	}
	public FileTreeString(Dir dir, Collection<? extends FileEntity> containsIn) {
		this(dir, FileUtils.containsInFilter(containsIn));
	}
	
	public void render(Appendable sink) throws IOException {
		appendDetails(dir, new char[0], sink);
		walk(new char[] {' ', '|'}, dir, sink);
	}
	
	private void walk(final char[] separator, Dir dir, Appendable sink) throws IOException {
		for (FileEntity f : dir) {
			if(filter.test(f)) {
				appendDetails(f, separator, sink);
				
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

					walk(sp2, asDir(f), sink);
				}
			}
		}
	}
	private Dir asDir(FileEntity f) {
		return (Dir)f;
	}
	
	private final StringBuilder buffer = new StringBuilder(); 
	
	private void appendDetails(FileEntity f, char[] separator, Appendable sink) throws IOException {
		append(separator, sink)
		.append(f.getName());
		
		Attrs ak = f.getSourceAttrs();
		long size = ak.size();

		if(!f.isDirectory() ? size <= 0 : ( f.isDirectory() && asDir(f).childrenCount() == 0)) 
			sink.append('\n');
		else {
			sink.append('\t')
			.append('(');

			if(size > 0) {
				buffer.setLength(0);
				MyUtilsBytes.bytesToHumanReadableUnits(size, false, buffer);
				sink.append(buffer);
			}
			
			if(f.isDirectory() && asDir(f).childrenCount() != 0)
				sink.append(' ').append(Utils.toString(asDir(f).childrenCount())).append(" files");

			sink.append(")\n");	
		}
	}
	
	private Appendable append(char[] separator, Appendable sink) throws IOException {
		for (char c : separator) 
			sink.append(c);
		return sink;
	}
}
