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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import sam.functions.IOExceptionConsumer;
import sam.io.BufferSupplier;
import sam.io.IOUtils;
import sam.io.serilizers.StringIOUtils;
import sam.io.serilizers.WriterImpl;
import sam.myutils.Checker;
import sam.nopkg.Resources;

class FileNamesHandler {
	private final Path path;
	private static final char SEPARATOR = '\n';

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
		ByteBuffer buffer = r.buffer();
		CharsetEncoder encoder = r.encoder();
		CharBuffer chars = r.chars();

		encoder.reset();
		buffer.clear();
		chars.clear();

		try (OutputStream _os = Files.newOutputStream(path, WRITE, CREATE, APPEND);
				GZIPOutputStream gos = new GZIPOutputStream(_os);
				WriterImpl w = new WriterImpl(b -> IOUtils.write(buffer, gos, false), buffer, chars, false, encoder)) {

			while (files.hasNext()) {
				String f = files.next();
				if(Checker.isNotEmpty(f))
					w.append(f);
				w.append(SEPARATOR);
			}
		}
	}

	void read(Resources r, IOExceptionConsumer<String> consumer) throws IOException {
		try (InputStream _is = Files.newInputStream(path, READ); 
				GZIPInputStream gis = new GZIPInputStream(_is);) {

			StringBuilder sb = r.sb();
			ByteBuffer buffer = r.buffer();
			CharsetDecoder decoder = r.decoder();
			CharBuffer chars = r.chars();

			chars.clear();
			sb.setLength(0);
			decoder.reset();
			buffer.clear();
			
			StringIOUtils.collect(BufferSupplier.of(gis, buffer), SEPARATOR, consumer, decoder, chars, sb);
		}
	}
}
