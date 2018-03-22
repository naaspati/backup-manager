package sam.backup.manager.file;

import java.io.DataOutputStream;
import java.io.IOException;

public class FileTreeWriter implements AutoCloseable {
	private final DataOutputStream dos;
 
	public FileTreeWriter(DataOutputStream dos) {
		this.dos = dos;
	}
	public void write(FileTreeEntity tree) throws IOException {
		dos.writeBoolean(tree.isDirectory());
		dos.writeUTF(tree.getfileNameString());
		
		writeAttrs(tree.getSourceAttrs());
		writeAttrs(tree.getBackupAttrs());
		
		if(tree.isDirectory()) {
			int c = tree.castDir().size();
			dos.writeInt(c);
			
			if(c != 0) {
				for (FileTreeEntity f : tree.castDir())
					write(f);
			}
		}
	}
	private void writeAttrs(AttrsKeeper ak) throws IOException {
		Attrs a = ak.getOld();
		dos.writeLong(a.modifiedTime);
		dos.writeLong(a.size);
		
	}
	@Override
	public void close() throws IOException {
		dos.close();
	}

}
