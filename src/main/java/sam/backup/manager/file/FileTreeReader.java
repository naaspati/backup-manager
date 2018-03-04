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
		private final String pathString;
		private final long lastModified;
		private final long size;
		private final int childCount;

		private Values() throws IOException {
			this.directory = dis.readBoolean();
			this.pathString = dis.readUTF();
			this.lastModified = dis.readLong();
			this.size = dis.readLong();
			this.childCount = directory ? dis.readInt() : -1;
		}
		public int getChildCount() {
			return childCount;
		}
		boolean isDirectory() {
			return directory;
		}
		String getPathString() {
			return pathString;
		}
		long getLastModified() {
			return lastModified;
		}
		long getSize() {
			return size;
		}
		@Override
		public String toString() {
			return new StringBuilder()
			.append("Values [directory=").append(directory)
			.append(", pathString=").append(pathString)
			.append(", lastModified=").append(lastModified) 
			.append(", size=").append(size)
			.append(", childCount=").append(childCount)
			.append("]")
			.toString();
		}
	}


	@Override
	public void close() throws IOException {
		dis.close();
	}
}
