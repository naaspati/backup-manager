package sam.backup.manager.file;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import sam.backup.manager.config.Config;

@Deprecated
public class FileTreeReader {
	private DataInputStream dis;
	
	public FileTree read(Path fileTreePath, Config config) throws IOException {
		try(InputStream gis = Files.newInputStream(fileTreePath, StandardOpenOption.READ);
				DataInputStream dis = new DataInputStream(gis);) {
			this.dis = dis;
			return readFileTree(config); 
		}
	}
	
	private FileTree readFileTree(Config config) throws IOException {
		dis.readBoolean();
		FileTree ft = new FileTree(config, dis.readUTF(), readAttr(), readAttr());
		readChildren(ft);
		return ft;
		
	}
	private DirEntity readDir(DirEntity parent) throws IOException {
		DirEntity dir = new DirEntity(dis.readUTF(), parent, readAttr(), readAttr());
		readChildren(dir);
		return dir;
	}
	private void readChildren(DirEntity parent) throws IOException {
		int size = dis.readInt();
		if(size == 0)
			return;
		parent.setSize(size);
		
		for (int i = 0; i < size; i++)
			parent.add(dis.readBoolean() ? readDir(parent) : new FileImpl(dis.readUTF(), parent, readAttr(), readAttr()));
	}
	private Attrs readAttr() throws IOException {
		return new Attrs(dis.readLong(), dis.readLong());
	}
}
