package sam.backup.manager.config.impl;

import java.io.File;
import java.nio.file.Files;
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
		return string();
	}
	public PathWrap resolve(String child) {
		if(path != null)
			return new PathWrap(path.resolve(child));
		
		return new PathWrap(concat(string, child));
	}
	public PathWrap resolve(Path child) {
		return of(path().resolve(child));
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
	public boolean exists() {
		return path == null ? new File(string).exists() : Files.exists(path);
	}
	public static PathWrap of(String string) {
		return new PathWrap(string);
	}
	public static PathWrap of(Path path) {
		return new PathWrap(path);
	}
}
