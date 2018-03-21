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
		private final int childCount;

		private Values() throws IOException {
			this.directory = dis.readBoolean();
			this.filename = dis.readUTF();
			this.src = new Attrs(dis.readLong(), dis.readLong());
			//TODO this.backup = new Attrs(dis.readLong(), dis.readLong());
			this.backup = new Attrs(0, 0);
			this.childCount = directory ? dis.readInt() : -1;
		}
		public int getChildCount() {
			return childCount;
		}
		boolean isDirectory() {
			return directory;
		}
		String getFileNameString() {
			return filename;
		}
		public Attrs sourceAttrs() {
			return src;
		}
		public Attrs backupAttrs() {
			return backup;
		}
	}
	@Override
	public void close() throws IOException {
		dis.close();
	}
}
