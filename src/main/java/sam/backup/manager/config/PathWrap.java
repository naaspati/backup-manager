package sam.backup.manager.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import sam.myutils.Checker;

public class PathWrap {
	private String string;
	private Path path;

	public PathWrap(Path path) {
		Checker.requireNonNull("path", path);
		this.path = path;
	}
	public PathWrap(String string) {
		Checker.requireNonNull("string", string);
		this.string = string;
	}

	public Path path() {
		if(path == null)
			path = Paths.get(string);
		return path;
	} 
	public String string() {
		if(string == null)
			string = path.toString();
		return string;
	}
	@Override
	public String toString() {
		return (string == null ? path : string).toString();
	}
	public PathWrap resolve(String child) {
		if(path != null)
			return new PathWrap(path.resolve(child));
		
		return new PathWrap(concat(string, child));
	}

	private static final StringBuilder sb = new StringBuilder();

	private static String concat(String parent, String child) {
		if(Checker.isEmptyTrimmed(child))
			throw new IllegalArgumentException("invalid child value");
		
		Checker.requireNonNull("parent", parent);
		
		synchronized (sb) {
			sb.setLength(0);
			sb.append(parent);
			if(sb.length() != 0) {
				if(!isSlash(sb, sb.length() - 1))
					sb.append('\\');
			}
			
			if(isSlash(child, 0))
				sb.append(child, 1, child.length());
			else
				sb.append(child);
			
			return sb.toString();
		}
	}
	private static boolean isSlash(CharSequence cs, int i) {
		char c = cs.charAt(i);
		return c == '\\' || c == '/';
	}
}
