package sam.backup.manager;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.Writable;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.io.serilizers.StringWriter2;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.SavedResource;

@Singleton
class UtilsImpl implements IUtils, ErrorHandlerRequired, Stoppable {
	private static final EnsureSingleton singleton = new EnsureSingleton();

	private final Logger logger = LogManager.getLogger(UtilsImpl.class);
	public final Path app_data = Paths.get("app_data");
	public final Path temp_dir;
	private final Supplier<String> counter;
	public final boolean SAVE_EXCLUDE_LIST = System2.lookupBoolean("SAVE_EXCLUDE_LIST", true);
	private BiConsumer<Object, Exception> errorHandler = (o, e) -> {throw new RuntimeException(e);};

	@Override
	public Path appDataDir() {
		return app_data;
	}
	@Override
	public boolean isSaveExcludeList() {
		return SAVE_EXCLUDE_LIST;
	}
	@Override
	public Path tempDir() {
		return temp_dir;
	}

	public UtilsImpl() throws IOException {
		singleton.init();

		String dt = MyUtilsPath.pathFormattedDateTime();
		String dir = Stream.of(MyUtilsPath.TEMP_DIR.toFile().list())
				.filter(s -> s.endsWith(dt))
				.findFirst()
				.orElse(null);

		if(dir != null) {
			temp_dir = MyUtilsPath.TEMP_DIR.resolve(dir);
		} else {
			int n = number(MyUtilsPath.TEMP_DIR);
			temp_dir = MyUtilsPath.TEMP_DIR.resolve((n+1)+" - "+MyUtilsPath.pathFormattedDateTime());
			Files.createDirectories(temp_dir);				
		}

		counter = new Supplier<String>() {
			AtomicInteger n = new AtomicInteger(number(temp_dir));

			@Override
			public String get() {
				return n.incrementAndGet()+" - ";
			}
		};

		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> logger.error("thread: {}", thread.getName(), exception));
	}
	private int number(Path path) {
		if(Files.notExists(path)) return 0;

		Pattern p = Pattern.compile("^(\\d+) - "); 

		return Stream.of(path.toFile().list())
				.map(p::matcher)
				.filter(Matcher::find)
				.map(m -> m.group(1))
				.mapToInt(Integer::parseInt)
				.max()
				.orElse(0);
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
			return String.valueOf((int) d);
		else
			return String.valueOf(d);
	}

	@Override
	public String millisToString(long millis) {
		if (millis <= 0)
			return "N/A";
		return durationToString(Duration.ofMillis(millis));
	}

	private final StringBuilder sb = new StringBuilder();

	@Override
	public String durationToString(Duration d) {
		synchronized (sb) {
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
	public String millsToTimeString(Long d) {
		return d == null || d <= 0 ? "--"
				: LocalDateTime.ofInstant(Instant.ofEpochMilli(d), ZoneOffset.of("+05:30"))
				.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	private Runnable backupLastPerformed_mod;
	private final SavedResource<Map<String, Long>> backupLastPerformed = new SavedResource<Map<String,Long>>() {
		private final Path path = app_data.resolve("backup-last-performed.dat");

		{
			backupLastPerformed_mod = this::increamentMod;
		}

		@Override
		public void write(Map<String, Long> data) {
			try {
				logger.debug("write {}", path);
				ObjectWriter.writeMap(path, data, DataOutputStream::writeUTF, DataOutputStream::writeLong);
			} catch (IOException e) {
				logger.warn("failed to save: {}", path, e);
			}
		}

		@Override
		public Map<String, Long> read() {
			if(Files.notExists(path))
				return new HashMap<>();

			try {
				logger.debug("READ {}", path);
				return ObjectReader.readMap(path, d -> d.readUTF(), DataInputStream::readLong);
			} catch (IOException e) {
				logger.warn("failed to read: {}", path, e);
				return new HashMap<>();
			}
		}
	};  

	@Override
	public Long getBackupLastPerformed(String key) {
		return backupLastPerformed.get().get(key);
	}
	@Override
	public void putBackupLastPerformed(String key, long time) {
		backupLastPerformed.get().put(key, time);
		backupLastPerformed_mod.run();
	}
	@Override
	public String hashedName(Path p, String ext) {
		return p.getFileName() + "-" + p.hashCode() + ext;
	}

	@Override
	public void write(Path path, CharSequence data) throws IOException {
		StringWriter2.setText(path, data);
	}


	@Override
	public void writeInTempDir0(Config config, String prefix, String suffix, CharSequence data, Logger logger) throws IOException {
		String name = counter.get() +
				(prefix == null ? "" : "-"+prefix+"-")+
				config.getName()+
				(suffix == null ? "" : "-"+suffix)+
				".txt";

		Path path = temp_dir.resolve(name);

		try {
			write(path, data);
			logger.info("created: "+path);
		} catch (Exception e) {
			throw new IOException(e.getMessage()+" ("+"failed to write: "+path+")", e);
		}
	}
	@Override
	public void writeInTempDir(Config config, String prefix, String suffix, CharSequence data, Logger logger) {
		try {
			writeInTempDir0(config, prefix, suffix, data, logger);
		} catch (IOException e) {
			errorHandler.accept("failed to save", e);
		}
	}
	@Override
	public void stop() throws IOException {
		backupLastPerformed.close();
	}
	@Override
	public Logger getLogger(Class<?> cls) {
		return LogManager.getLogger(cls);
	}

	@Override
	public boolean saveInTempDirHideError(Writable w, Config config, String directory, String fileName) {
		try {
			saveInTempDir(w, config, directory, fileName);
			return true;
		} catch (IOException e) {
			errorHandler.accept(path(temp_dir, directory, config.getName(), fileName), e);
			return false;
		}
	}

	@Override
	public Path saveInTempDir(Writable w, Config config, String directory, String fileName) throws IOException {
		Path p = path(temp_dir, directory, config.getName(), fileName);
		save(p, w);
		return p;
	}
	private Path path(Path root, String child1, String...child) {
		return root.resolve(Paths.get(child1, child));
	}

	private void save(Path p, Writable w) throws IOException {
		Files.createDirectories(p.getParent());

		try(BufferedWriter os = Files.newBufferedWriter(p, WRITE, CREATE, TRUNCATE_EXISTING)) {
			w.write(os);
		}
	}
	@Override
	public void setErrorHandler(BiConsumer<Object, Exception> errorHandler) {
		this.errorHandler = Objects.requireNonNull(errorHandler);
	}
	
	@Override
	public void setTextNoError(Path target, CharSequence content, String errorMessage) {
		try {
			StringWriter2.setText(target, content);
		} catch (IOException e) {
			errorHandler.accept(errorMessage+target, e);
		}
	}
}
