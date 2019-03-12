package sam.backup.manager.file;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.BitSet;

import sam.io.IOUtils;
import sam.nopkg.Resources;

class RemainingHandler {
	final Path path;
	
	public RemainingHandler(Path path) {
		this.path = path;
	}
	void write(ArrayWrap<FileImpl> files, Resources r) throws IOException {
		try (FileChannel fc = FileChannel.open(path, WRITE, TRUNCATE_EXISTING)) {
			ByteBuffer buffer = r.buffer();
			buffer.clear();

			fc.write(buffer);
			buffer.clear();
			buffer.putInt(files.size());
			BitSet isDir = new BitSet(files.size());
			
			for (int i = 0; i < files.size(); i++) {
				FileImpl f = files.get(i);

				writeIf(buffer, fc, Integer.BYTES);

				if(f == null)
					buffer.putInt(-1);
				else {
					buffer.putInt(f.getParent() == null ? -10 : f.getParent().id);
					if(f.isDirectory())
						isDir.set(i);
				}
			}

			long[] longs = isDir.toLongArray();

			writeIf(buffer, fc, Integer.BYTES);

			buffer.putInt(longs.length);

			for (long c : longs) {
				writeIf(buffer, fc, Long.BYTES);
				buffer.putLong(c);
			}

			IOUtils.write(buffer, fc, true);
		}
	}
	
	private void writeIf(ByteBuffer buffer, FileChannel fc, int bytes) throws IOException {
		if(buffer.remaining() < bytes)
			IOUtils.write(buffer, fc, true);
	}
	
	int[] parents;
	BitSet isDir;
	
	public void read(ByteBuffer buffer) throws IOException {
		try (FileChannel fc = FileChannel.open(path, READ)) {
			buffer.clear();

			fc.read(buffer);
			buffer.clear();

			parents = new int[buffer.getInt()];
			for (int i = 0; i < parents.length; i++) {
				readIf(fc, buffer, Integer.BYTES);
				parents[i] = buffer.getInt();
			}

			readIf(fc, buffer, Long.BYTES);

			long[] isDir = new long[buffer.getInt()];

			for (int i = 0; i < parents.length; i++) {
				readIf(fc, buffer, Long.BYTES);
				isDir[i] = buffer.getLong();
			}
			
			this.isDir = BitSet.valueOf(isDir);
		}
	}
	
	private static void readIf(FileChannel fc, ByteBuffer buffer, int bytes) throws IOException {
		if(buffer.remaining() < bytes) {
			IOUtils.compactOrClear(buffer);
			fc.read(buffer);
			buffer.flip();
		}
	}
	

}
