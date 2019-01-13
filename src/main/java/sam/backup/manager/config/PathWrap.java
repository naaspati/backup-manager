package sam.backup.manager.config;

import java.nio.file.Path;

public class PathWrap {
	private final String raw;
	private final Path path;
	
	public PathWrap(Path path, String raw) {
		this.path = path;
		this.raw = raw;
	}
	
	public Path path() {
		return path;
	} 
	public String raw() {
		return raw;
	}

	@Override
	public String toString() {
		return "PathWrap [raw=\"" + raw + "\", path=\"" + path + "\"]";
	}
}
