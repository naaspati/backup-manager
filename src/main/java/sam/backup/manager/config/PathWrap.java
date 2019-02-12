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
}
