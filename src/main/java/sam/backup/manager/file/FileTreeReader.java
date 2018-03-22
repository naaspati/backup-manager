package sam.backup.manager.file;

import java.io.DataInputStream;
import java.io.IOException;

class FileTreeReader implements AutoCloseable {
	private final DataInputStream dis;

	FileTreeReader(DataInputStream dis) {
		this.dis = dis;
	}
	Values next() throws IOException {
		return new Values(); 
	}
	public final class Values {
		private final boolean directory;
		private final String filename;
		private final Attrs src;
		private final Attrs backup;
		private final int size;

		private Values() throws IOException {
			this.directory = dis.readBoolean();
			this.filename = dis.readUTF();
			this.src = new Attrs(dis.readLong(), dis.readLong());
			this.backup = new Attrs(dis.readLong(), dis.readLong());
			this.size = directory ? dis.readInt() : -1;
		}
		public boolean isDirectory() { return directory; }
		public String getFilenameString() { return filename; }
		public Attrs getSrcAttrs() { return src; }
		public Attrs getBackupAttrs() { return backup; }
		public int size() { return size; }
	}
	@Override
	public void close() throws IOException {
		dis.close();
	}
}
