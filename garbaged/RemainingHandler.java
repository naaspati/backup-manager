package sam.backup.manager.file;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.backup.manager.file.WithId.id;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.BitSet;

import sam.backup.manager.file.api.Dir;
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
			int dir_count = 0;
			
			final int size = files.size();
			for (int i = 0; i < size; i++) {
				FileImpl f = files.get(i);

				writeIf(buffer, fc, Integer.BYTES);

				if(f == null)
					buffer.putInt(-1);
				else {
					buffer.putInt(f.getParent() == null ? -10 : id(f.getParent()));
					if(f.isDirectory()) {
						dir_count++;
						isDir.set(i);
					}
				}
			}

			long[] longs = isDir.toLongArray();

			writeIf(buffer, fc, Integer.BYTES);
			buffer.putInt(longs.length);

			for (long c : longs) {
				writeIf(buffer, fc, Long.BYTES);
				buffer.putLong(c);
			}
			
			writeIf(buffer, fc, Integer.BYTES);
			buffer.putInt(dir_count * 2);
			
			for (int i = 0; i < size; i++) {
				FileImpl f = files.get(i);
				
				writeIf(buffer, fc, Integer.BYTES * 2);

				if(f != null && f.isDirectory())
					buffer.putInt(i).putInt(((Dir)f).childrenCount());
			}

			IOUtils.write(buffer, fc, true);
			
		}
	}
	
	private void writeIf(ByteBuffer buffer, FileChannel fc, int bytes) throws IOException {
		if(buffer.remaining() < bytes)
			IOUtils.write(buffer, fc, true);
	}
	
	/**
	 * parents[child_id] = parent_id
	 */
	int[] parents; 
	/**
	 * size[n] = child_id
	 * size[n+1] = child_count
	 * 
	 */
	int[] sizes;
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

			readIf(fc, buffer, Integer.BYTES);

			long[] isDir = new long[buffer.getInt()];

			for (int i = 0; i < parents.length; i++) {
				readIf(fc, buffer, Long.BYTES);
				isDir[i] = buffer.getLong();
			}
			
			this.isDir = BitSet.valueOf(isDir);
			
			readIf(fc, buffer, Integer.BYTES);
			
			sizes = new int[buffer.getInt()];
			
			int n = 0;
			while(n < sizes.length) {
				sizes[n++] = buffer.getInt();
				sizes[n++] = buffer.getInt();
			}
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
