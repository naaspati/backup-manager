package sam.backup.manager;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.functions.IOExceptionConsumer;
import sam.io.fileutils.FileNameSanitizer;
import sam.io.serilizers.WriterImpl;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Resources;
import sam.reference.WeakAndLazy;

@Singleton
class UtilsImpl implements IUtils, ErrorHandlerRequired {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	private final Logger logger = getLogger(UtilsImpl.class);

	{
		singleton.init();
	}

	private BiConsumer<Object, Exception> errorHandler = (o, e) -> {throw new RuntimeException(e);};
	private Path temp_dir;
	private Supplier<String> counter;

	@Override
	public void setAppConfig(AppConfig config) {
		this.temp_dir = config.tempDir();

		counter = new Supplier<String>() {
			AtomicInteger n = new AtomicInteger(Utils.number(temp_dir));

			@Override
			public String get() {
				return n.incrementAndGet()+" - ";
			}
		};

	}

	@Override
	public String bytesToString(long bytes) {
		if (bytes == 0)
			return "0";
		if (bytes < 1048576)
			return bytesToString(bytes, 1024) + "KB";
		if (bytes < 1073741824)
			return bytesToString(bytes, 1048576) + "MB";
		else
			return bytesToString(bytes, 1073741824) + "GB";

	}
	private String bytesToString(long bytes, long divisor) {
		double d = divide(bytes, divisor);
		if (d == (int) d)
			return toString((int) d);
		else
			return Double.toString(d);
	}

	@Override
	public String millisToString(long millis) {
		if (millis <= 0)
			return "N/A";
		return durationToString(Duration.ofMillis(millis));
	}

	private final WeakAndLazy<StringBuilder> wsb = new WeakAndLazy<>(StringBuilder::new);

	@Override
	public String durationToString(Duration d) {
		synchronized (wsb) {
			StringBuilder sb = wsb.get();
			sb.setLength(0);

			char[] chars = d.toString().toCharArray();
			for (int i = 2; i < chars.length; i++) {
				char c = chars[i];
				switch (c) {
					case 'H':
						sb.append("hours ");
						break;
					case 'M':
						sb.append("min ");
						break;
					case 'S':
						sb.append("sec");
						break;
					case '.':
						sb.append("sec");
						return sb.toString();
					default:
						sb.append(c);
						break;
				}
			}
			return sb.toString();
		}
	}

	@Override
	public double divide(long dividend, long divisor) {
		if (divisor == 0 || dividend == 0)
			return 0;
		return (dividend * 100 / divisor) / 100D;
	}

	@Override
	public String millsToTimeString(long d) {
		return d <= 0 ? "--"
				: LocalDateTime.ofInstant(Instant.ofEpochMilli(d), ZoneOffset.of("+05:30"))
				.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	@Override
	public void setErrorHandler(BiConsumer<Object, Exception> errorHandler) {
		this.errorHandler = Objects.requireNonNull(errorHandler);
	}
	private final String[] intStringCache = new String[100]; 

	@Override
	public String toString(int n) {
		if(n < 0 || n >= intStringCache.length)
			return Integer.toString(n);

		String s = intStringCache[n];

		if(s == null)
			return s = intStringCache[n] = Integer.toString(n);

		return s;
	}
	@Override
	public void write(Path path, boolean append, IOExceptionConsumer<WriterImpl> consumer) throws IOException {
		try(FileChannel fc = fileChannel(path, append);
				Resources r = Resources.get();
				WriterImpl w = new WriterImpl(fc, r.buffer(), r.chars(), false, r.encoder())) {
			consumer.accept(w);
			logger.debug("WRITE {}", path);
		}
	}
	@Override
	public void writeHandled(Path path, boolean append, IOExceptionConsumer<WriterImpl> consumer) {
		try {
			write(path, append, consumer);
		} catch (IOException e) {
			errorHandler.accept(path, e);
		}
	}
	@Override
	public FileChannel fileChannel(Path path, boolean append) throws IOException {
		return FileChannel.open(path, WRITE, CREATE, append ? APPEND : TRUNCATE_EXISTING);
	}
	@Override
	public Logger getLogger(Class<?> cls) {
		return LogManager.getLogger(cls);
	}
	@Override
	public Path tempDir() {
		return temp_dir;
	}
	
	private final Map<Config, Path> pathMap = new IdentityHashMap<>();
	
	@Override
	public Path tempDirFor(Config config) {
		Objects.requireNonNull(config);
		
		Path p = pathMap.get(config);
		if(p == null) {
			p = tempDir().resolve(FileNameSanitizer.sanitize(config.getType()+"-"+config.getName()));
			try {
				Files.createDirectories(p);
				logger.debug("dir created: {}", p);
			} catch (IOException e) {
				throw new RuntimeException("failed to create Dir: "+p, e);
			}
			pathMap.put(config, p);
		}
		return p;
	}	 
}
