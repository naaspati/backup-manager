package sam.backup.manager.file;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class FileTreeWriter implements AutoCloseable {
	private final DataOutputStream dos;
 
	public FileTreeWriter(DataOutputStream dos) {
		this.dos = dos;
	}
	public void write(FileTreeEntity tree) throws IOException {
		dos.writeBoolean(tree.isDirectory());
		dos.writeUTF(tree.getPathString());
		dos.writeLong(tree.getModifiedTime());
		dos.writeLong(tree.getSize());
		
		if(tree.isDirectory()) {
			List<FileTreeEntity> children = tree.castDir().getChildren();
			dos.writeInt(children.size());
			
			if(!children.isEmpty()) {
				for (FileTreeEntity f : children)
					write(f);
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		dos.close();
	}

}
