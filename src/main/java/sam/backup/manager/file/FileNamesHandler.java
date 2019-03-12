package sam.backup.manager.file;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import sam.io.IOUtils;
import sam.myutils.Checker;
import sam.nopkg.Resources;

class FileNamesHandler {
	private final Path path;

	public FileNamesHandler(Path path) {
		this.path = path;
	}

	void write(Resources r, ArrayWrap<FileImpl> files) throws IOException {
		write(r, new Iterator<String>() {
			int i = files.oldSize();
			int max = files.size();

			@Override
			public String next() {
				if (i >= max)
					throw new NoSuchElementException();

				FileImpl f = files.get(i++);
				return f == null ? null : f.filename;
			}

			@Override
			public boolean hasNext() {
				return i < max;
			}
		});
	}

	void write(Resources r, Iterator<String> files) throws IOException {
		StringBuilder sb = r.sb();
		ByteBuffer buffer = r.buffer();
		CharsetEncoder encoder = r.encoder();
		CharBuffer chars = r.chars();
		byte[] bytes = r.bytes();

		encoder.reset();
		sb.setLength(0);
		buffer.clear();
		chars.clear();

		try (OutputStream _os = Files.newOutputStream(path, WRITE, CREATE, APPEND);
				GZIPOutputStream gos = new GZIPOutputStream(_os);) {

			CharBuffer separator = CharBuffer.allocate(1);
			separator.put('\t');

			while (files.hasNext()) {
				String f = files.next();
				if (f != null)
					encode(CharBuffer.wrap(f), buffer, bytes, encoder, gos);

				separator.clear();
				encode(separator, buffer, bytes, encoder, gos);
			}

			write(buffer, bytes, gos);
			encoder.flush(buffer);
			write(buffer, bytes, gos);
		}
	}

	private void encode(CharBuffer cb, ByteBuffer buffer, byte[] bytes, CharsetEncoder encoder, GZIPOutputStream gos)
			throws IOException {
		CoderResult c = encoder.encode(cb, buffer, false);

		while (cb.hasRemaining()) {
			if (c.isOverflow())
				write(buffer, bytes, gos);
			else if (!c.isUnderflow())
				c.throwException();
		}

	}

	private void write(ByteBuffer buffer, byte[] bytes, OutputStream gos) throws IOException {
		buffer.flip();
		if (buffer.hasRemaining())
			gos.write(bytes, 0, buffer.limit());

		buffer.clear();
	}

	String[] read(Resources r, final int count) throws IOException {
		String[] filenames = new String[count];
		int index = 0;

		try (InputStream _is = Files.newInputStream(path, READ); GZIPInputStream gis = new GZIPInputStream(_is);) {

			StringBuilder sb = r.sb();
			ByteBuffer buffer = r.buffer();
			byte[] bytes = r.bytes();
			CharsetDecoder decoder = r.decoder();
			CharBuffer chars = r.chars();

			chars.clear();
			sb.setLength(0);
			decoder.reset();
			IOUtils.setFilled(buffer);

			loop1: 
				while (true) {
					IOUtils.compactOrClear(buffer);
					final int read = gis.read(bytes, buffer.position(), buffer.remaining());
					
					if (read != -1) {
						buffer.limit(buffer.position() + read);
						buffer.position(0);
					}

					while (true) {
						CoderResult res = buffer.hasRemaining() ? decoder.decode(buffer, chars, read == -1) : CoderResult.UNDERFLOW;

						if (read == -1 && res.isUnderflow()) {
							index = process(sb, filenames, chars, index);
							decoder.flush(chars);
							index = process(sb, filenames, chars, index);
							break loop1;
						}

						if (res.isUnderflow())
							break;
						else if (res.isOverflow())
							index = process(sb, filenames, chars, index);
						else
							res.throwException();
					}
				}

			Checker.assertTrue(index == filenames.length);
			return filenames;
		}
	}

	private static int process(StringBuilder sb, String[] filenames, CharBuffer chars, int index) {
		chars.flip();

		while (chars.hasRemaining()) {
			char c = chars.get();
			if (c == '\t') {
				filenames[index++] = sb.length() == 0 ? null : sb.toString();
				sb.setLength(0);
			}
		}
		chars.clear();
		return index;
	}

}
