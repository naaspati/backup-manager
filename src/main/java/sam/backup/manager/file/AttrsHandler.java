package sam.backup.manager.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.BitSet;

import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.io.IOUtils;
import sam.myutils.Checker;
import sam.nopkg.Resources;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.backup.manager.file.api.FileTree.*;

class AttrsHandler {

	private static final int ATTRS_BYTES = 
			Integer.BYTES + // id
			(Long.BYTES + Integer.BYTES) * 2; // src_attr + backup_attr

	private final Path path;

	public AttrsHandler(Path path) {
		this.path = path;
	}
	
	Attr[] src, backup;

	void read(Resources r, String[] filenames) throws IOException {
		ByteBuffer buffer = r.buffer();
		buffer.clear();

		src = new Attr[filenames.length];
		backup = new Attr[filenames.length];

		try (FileChannel fc = FileChannel.open(path, READ)) {
			while (fc.read(buffer) != -1) {
				buffer.flip();
				while (buffer.remaining() > ATTRS_BYTES) {
					int id = buffer.getInt();
					Attr s = readAttr(buffer);
					Attr b = readAttr(buffer);

					if (s == DELETED_ATTR || b == DELETED_ATTR)
						filenames[id] = null;
					else {
						src[id] = s;
						backup[id] = b;
					}
				}
				IOUtils.compactOrClear(buffer);
			}
			Checker.assertTrue(buffer.remaining() == 0);
		}
	}

	private static void put(ByteBuffer buffer, Attrs attrs) {
		Attr t = attrs == null ? null : attrs.current();
		if (t == null)
			t = EMPTY_ATTR;

		put(buffer, t);
	}

	private static void put(ByteBuffer b, Attr attr) {
		b.putLong(attr.lastModified).putInt(attr.size);
	}

	private static Attr readAttr(ByteBuffer buffer) {
		long lasmod = buffer.getLong();
		int size = buffer.getInt();

		if (lasmod == EMPTY_ATTR_MARKER || size == EMPTY_ATTR_MARKER)
			return EMPTY_ATTR;
		if (lasmod == DELETED_ATTR_MARKER || size == DELETED_ATTR_MARKER)
			return DELETED_ATTR;

		return new Attr(lasmod, size);
	}
	
	void write(BitSet attrsMod, FileImpl[] data, ByteBuffer buffer) throws IOException {
		buffer.clear();
		
		final ByteBuffer deleted = ByteBuffer.allocate(ATTRS_BYTES - 4);
		put(deleted, DELETED_ATTR);
		put(deleted, DELETED_ATTR);

		try (FileChannel fc = FileChannel.open(path, WRITE, APPEND)) {
			for (int i = 0; i < data.length; i++) {
				if (!attrsMod.get(i))
					continue;

				if (buffer.remaining() < ATTRS_BYTES)
					IOUtils.write(buffer, fc, true);

				FileImpl f = data[i];

				if (f == null) {
					deleted.clear();
					buffer.put(deleted);
				} else {
					put(buffer, f.getSourceAttrs());
					put(buffer, f.getBackupAttrs());
				}
			}

			IOUtils.write(buffer, fc, true);
		}
	}
}
