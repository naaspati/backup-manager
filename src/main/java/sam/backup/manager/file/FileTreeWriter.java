package sam.backup.manager.file;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileTreeWriter  {
	private DataOutputStream dos;

	public void write(Path path, FileTree tree) throws IOException {
		try (OutputStream gos = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				// GZIPOutputStream gos = new GZIPOutputStream(os);
				DataOutputStream dos = new DataOutputStream(gos);
				){
			this.dos = dos;
			write(tree);
		}
	}

	private void write(FileTreeEntity tree) throws IOException {
		dos.writeBoolean(tree.isDirectory());
		dos.writeUTF(tree.getfileNameString());

		writeAttrs(tree.getSourceAttrs());
		writeAttrs(tree.getBackupAttrs());

		if(tree.isDirectory()) {
			DirEntity dir = tree.asDir();
			dos.writeInt(dir.count());

			if(!dir.isEmpty()) {
				for (FileTreeEntity f : dir)
					write(f);
			}
		}
	}
	private void writeAttrs(AttrsKeeper ak) throws IOException {
		Attrs a = ak.getOld();
		dos.writeLong(a == null ? 0 : a.modifiedTime);
		dos.writeLong(a == null ? 0 : a.size);
	}
}
