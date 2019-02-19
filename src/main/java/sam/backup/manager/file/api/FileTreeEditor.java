package sam.backup.manager.file.api;

import java.io.IOException;
import java.nio.file.Path;

import sam.backup.manager.walk.WalkMode;

/* Currently serves as a general purpose Class
 * in future will modify it to serve as a transectional unit.
 * 
 * @author Sameer
 *
 */
public interface FileTreeEditor extends AutoCloseable {
	FileEntity addFile(Path file, Attr af, WalkMode walkMode);
	Dir addDir(Path dir, Attr attr, WalkMode walkMode);
	
	FileEntity addFile(Dir parent, String filename);
	Dir addDir(Dir parent, String dirname);
	
	void setWalked(Dir dir, boolean walked);
	boolean isWalked(Dir dir);
	void setAttr(Attr attr, WalkMode walkMode, Path dir);
	void close() throws IOException;
}
