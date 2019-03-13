package sam.backup.manager.file;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.functions.IOExceptionConsumer;
import sam.io.BufferSupplier;
import sam.io.IOUtils;
import sam.io.serilizers.StringIOUtils;
import sam.myutils.Checker;
import sam.nopkg.Resources;

class MetaHandler {
	private static final Logger logger = LogManager.getLogger(MetaHandler.class);
	
	private final Path path;
	private final int tree_id;
	
	public MetaHandler(Path path, int tree_id) {
		this.path = path;
		this.tree_id = tree_id;
	}

	void write(ArrayWrap<FileImpl> data, Resources r, Path sourceDirPath, Path backupDirPath) throws IOException {
		if (Files.notExists(path)) {
			StringBuilder sb = r.sb();
			ByteBuffer buffer = r.buffer();
			CharsetEncoder encoder = r.encoder();

			sb.setLength(0);
			buffer.clear();
			encoder.reset();

			buffer.putInt(tree_id).putInt(data.size());
			
			sb.append(sourceDirPath)
			.append('\t')
			.append(backupDirPath);

			try (FileChannel fc = FileChannel.open(path, CREATE_NEW, WRITE)) {
				StringIOUtils.write(StringIOUtils.writer(fc), sb, encoder, buffer);
			}
		} else {
			try (FileChannel fc = FileChannel.open(path, WRITE)) {
				ByteBuffer buffer = ByteBuffer.allocate(4);
				buffer.putInt(data.size()).flip();

				fc.write(buffer, 4);
				logger.debug("size increase  {} -> {}, for: {}", data.oldSize(), data.size(), this);
			}
		}
	}
	
	int validate(Resources r, Path sourceDirPath, Path backupDirPath)
			throws IOException {
		StringBuilder sb = r.sb();
		ByteBuffer buffer = r.buffer();
		CharsetEncoder encoder = r.encoder();

		sb.setLength(0);
		encoder.reset();
		buffer.clear();

		try (FileChannel fc = FileChannel.open(path, READ)) {
			if (fc.read(buffer) < 8)
				throw new FailedToCreateFileTree();

			buffer.flip();
			int id = buffer.getInt();

			if (id != tree_id)
				throw new FailedToCreateFileTree("invalid id. old: " + id + ", new: " + tree_id);

			int count = buffer.getInt();
			BufferSupplier supplier = new BufferSupplier() {
				int n = 0;
				boolean first = true;

				@Override
				public ByteBuffer next() throws IOException {
					if (first)
						return buffer;
					first = false;

					IOUtils.compactOrClear(buffer);
					n = fc.read(buffer);
					buffer.flip();

					return buffer;
				}

				@Override
				public boolean isEndOfInput() throws IOException {
					return n == -1;
				}
			};

			sb.setLength(0);
			int[] checked = { 0 };
			IOExceptionConsumer<CharBuffer> consumer = new IOExceptionConsumer<CharBuffer>() {
				boolean first = true;

				@Override
				public void accept(CharBuffer e) throws IOException {
					while (e.hasRemaining()) {
						char c = e.get();
						if (c == '\t') {
							if (first)
								check(sb.toString(), sourceDirPath, "sourceDirPath");
							else
								check(sb.toString(), backupDirPath, "backupDirPath");

							checked[0]++;
							first = false;
						} else
							sb.append(c);
					}
					e.clear();
				}

				private void check(String expected, Path actual, String tag) throws FailedToCreateFileTree {
					if (!Paths.get(expected).equals(actual))
						throw new FailedToCreateFileTree(
								"invalid " + tag + ". expected: " + expected + ", was: " + actual);
				}
			};

			Checker.assertTrue(checked[0] == 2);
			StringIOUtils.read(supplier, consumer, r.decoder(), r.chars());

			return count;
		}
	}
}
