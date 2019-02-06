package sam.backup.manager.extra;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static javafx.application.Platform.runLater;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sam.backup.manager.config.PathWrap;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.file.Dir;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeWalker;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxHyperlink;
import sam.fx.helpers.FxUtils;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.io.serilizers.StringWriter2;
import sam.myutils.Checker;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;
import sam.nopkg.Junk;
import sam.nopkg.LazyLoadedData;
import sam.reference.WeakAndLazy;

public final class Utils {
	private static final Logger LOGGER;
	public static final Path APP_DATA = Paths.get("app_data");
	public static final Path TEMP_DIR;
	private static final Supplier<String> counter;
	private static Window window;
	private static HostServices hostservice;
	private static BiConsumer<Object, Exception> errorHandler;
	private static final ExecutorService EXECUTOR_SERVICE;
	public static final boolean SAVE_EXCLUDE_LIST = System2.lookupBoolean("SAVE_EXCLUDE_LIST", true);

	static {
		try {
			String dt = MyUtilsPath.pathFormattedDateTime();
			String dir = Stream.of(MyUtilsPath.TEMP_DIR.toFile().list())
					.filter(s -> s.endsWith(dt))
					.findFirst()
					.orElse(null);

			if(dir != null) {
				TEMP_DIR = MyUtilsPath.TEMP_DIR.resolve(dir);
			} else {
				int n = number(MyUtilsPath.TEMP_DIR);
				TEMP_DIR = MyUtilsPath.TEMP_DIR.resolve((n+1)+" - "+MyUtilsPath.pathFormattedDateTime());
				Files.createDirectories(TEMP_DIR);				
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		counter = new Supplier<String>() {
			AtomicInteger n = new AtomicInteger(number(TEMP_DIR));

			@Override
			public String get() {
				return n.incrementAndGet()+" - ";
			}
		};

		LOGGER = Utils.getLogger(Utils.class);
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LOGGER.error("thread: {}", thread.getName(), exception));
		
		EXECUTOR_SERVICE = Optional.ofNullable(System2.lookup("THREAD_COUNT")).map(Integer::parseInt).filter(i -> i > 0).map(c -> Executors.newFixedThreadPool(c)).orElseGet(() -> Executors.newSingleThreadExecutor());
	}

	// does nothing only initiates static block
	public static void init() {}

	private static int number(Path path) {
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

	private Utils() {}

	public static String bytesToString(long bytes) {
		if (bytes == 0)
			return "0";
		if (bytes < 1048576)
			return bytesToString(bytes, 1024) + "KB";
		if (bytes < 1073741824)
			return bytesToString(bytes, 1048576) + "MB";
		else
			return bytesToString(bytes, 1073741824) + "GB";

	}

	private static String bytesToString(long bytes, long divisor) {
		double d = divide(bytes, divisor);
		if (d == (int) d)
			return String.valueOf((int) d);
		else
			return String.valueOf(d);
	}

	public static String millisToString(long millis) {
		if (millis <= 0)
			return "N/A";
		return durationToString(Duration.ofMillis(millis));
	}

	private static final StringBuilder sb = new StringBuilder();

	public static String durationToString(Duration d) {
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

	public static double divide(long dividend, long divisor) {
		if (divisor == 0 || dividend == 0)
			return 0;
		return (dividend * 100 / divisor) / 100D;
	}

	public static String millsToTimeString(Long d) {
		return d == null || d <= 0 ? "--"
				: LocalDateTime.ofInstant(Instant.ofEpochMilli(d), ZoneOffset.of("+05:30"))
				.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	private static void waitUntil(AtomicBoolean stopper) {
		while (!stopper.get()) { }
	}

	public static void showErrorAndWait(Object text, Object header, Exception e) {
		AtomicBoolean b = new AtomicBoolean();
		runLater(() -> {
			FxAlert.showErrorDialog(text, header, e);
			b.set(true);
		});
		waitUntil(b);
	}
	private static final LazyLoadedData<Map<String, Long>> backupLastPerformed = new LazyLoadedData<Map<String,Long>>() {
		private final Path path = APP_DATA.resolve("backup-last-performed.dat");
		
		@Override
		public void save() {
			try {
				ObjectWriter.writeMap(path, data, DataOutputStream::writeUTF, DataOutputStream::writeLong);
			} catch (IOException e) {
				LOGGER.warn("failed to save: {}", path, e);
			}
		}
		@Override
		public Map<String, Long> init() {
			if(Files.notExists(path))
				return new HashMap<>();

			try {
				return ObjectReader.readMap(path, d -> d.readUTF(), DataInputStream::readLong);
			} catch (IOException e) {
				LOGGER.warn("failed to read: {}", path, e);
				return new HashMap<>();
			}
		}
	};  
	
	public static Long getBackupLastPerformed(String key) {
		return backupLastPerformed.getData().get(key);
	}
	public static void putBackupLastPerformed(String key, long time) {
		backupLastPerformed.getData().put(key, time);
		backupLastPerformed.setModified(true);
	}
	public static Stage showStage(Parent content) {
		Stage stg = new Stage();
		stg.initModality(Modality.WINDOW_MODAL);
		stg.initStyle(StageStyle.UTILITY);
		stg.initOwner(window);
		stg.setScene(new Scene(content));
		stg.getScene().getStylesheets().setAll(window.getScene().getStylesheets());
		stg.setWidth(300);
		stg.setHeight(400);
		runLater(stg::show);

		return stg;
	}

	public static void showErrorDialog(Object text, String header, Exception error) {
		runLater(() -> FxAlert.showErrorDialog(text, header, error));
	}
	public static FileChooser selectFile(File expectedDir, String expectedName, String title) {
		return FxUtils.fileChooser(expectedDir, expectedName, title, null);
	}
	public static String hashedName(Path p, String ext) {
		return p.getFileName() + "-" + p.hashCode() + ext;
	}

	public static void write(Path path, CharSequence data) throws IOException {
		StringWriter2.setText(path, data);
	}


	public static void writeInTempDir0(Config config, String prefix, String suffix, CharSequence data, Logger logger) throws IOException {
		String name = counter.get() +
				(prefix == null ? "" : "-"+prefix+"-")+
				config.getName()+
				(suffix == null ? "" : "-"+suffix)+
				".txt";

		Path path = TEMP_DIR.resolve(name);

		try {
			write(path, data);
			logger.info("created: "+path);
		} catch (Exception e) {
			throw new IOException(e.getMessage()+" ("+"failed to write: "+path+")", e);
		}
	}
	public static void writeInTempDir(Config config, String prefix, String suffix, CharSequence data, Logger logger) {
		try {
			writeInTempDir0(config, prefix, suffix, data, logger);
		} catch (IOException e) {
			runLater(() -> FxAlert.showErrorDialog(null, "failed to save", e));
		}
	}

	public static void stop() throws InterruptedException {
		if (backupLastPerformed.isModified())
			backupLastPerformed.save();

		EXECUTOR_SERVICE.shutdownNow();
		System.out.println("waiting thread to die");
		EXECUTOR_SERVICE.awaitTermination(2, TimeUnit.SECONDS);
	}
	private static final WeakAndLazy<FXMLLoader> fxkeep = new WeakAndLazy<>(FXMLLoader::new);
	

	public static void fxml(String filename, Object root, Object controller) {
		try {
			FXMLLoader fx = fxkeep.get();
			fx.setLocation(ClassLoader.getSystemResource(filename));
			fx.setController(controller);
			fx.setRoot(root);
			fx.load();
		} catch (IOException e) {
			Utils.showErrorAndWait(null, "fxml failed: " + filename, e);
			System.exit(0);
		}
	}

	public static void fxml(Object parentclass, Object root, Object controller) {
		fxml("fxml/" + parentclass.getClass().getSimpleName() + ".fxml", root, controller);
	}

	public static void fxml(Object obj) {
		fxml(obj, obj, obj);
	}

	public static void stylesheet(Parent node) {
		String name = "stylesheet/" + node.getClass().getSimpleName() + ".css";
		URL url = ClassLoader.getSystemResource(name);
		if (url == null) {
			LOGGER.error("stylesheet not found: " + name);
			return;
		}
		node.getStylesheets().add(url.toExternalForm());
	}

	public static Node hyperlink(PathWrap wrap) {
		if(wrap != null && wrap.path() != null) {
			Hyperlink hyperlink = FxHyperlink.of(wrap.path());
			hyperlink.setMinWidth(400);
			return hyperlink;
		} else {
			Text t = new Text(wrap == null ? "--" : wrap.raw());
			t.setDisable(true);
			return t;
		}
	}
	public static Node hyperlink(List<PathWrap> wraps) {
		if(Checker.isEmpty(wraps))
			return hyperlink((PathWrap)null);
		if(wraps.size() == 1)
			return hyperlink(wraps.get(0));
		VBox box = new VBox(2);
		wraps.forEach(p -> box.getChildren().add(hyperlink(p)));
		
		return box;
	}
	public static <T> T either(T t1, T t2, T defaultValue) {
		if(t1 == null && t2 == null)
			return defaultValue;
		return t1 != null ? t1 : t2;
	}

	public static Node button(String tooltip, String icon, EventHandler<ActionEvent> action) {
		Button button = new Button();
		button.getStyleClass().clear();
		button.setTooltip(new Tooltip(tooltip));
		button.setGraphic(new ImageView(icon));
		button.setOnAction(action);
		return button;
	}
	public static Logger getLogger(Class<?> cls) {
		return LoggerFactory.getLogger(cls);
	}
	public static FileTree readFiletree(Config c, TreeType type, boolean createNewIfNotExists) throws Exception {
		//FIXME 
		return Junk.notYetImplemented();
		// return FileTree.getInstance().newFileTree(c, type, createNewIfNotExists);
	}
	
	public static boolean saveFileTree(Config config) {
		return Junk.notYetImplemented();
		//FIXME return saveFileTree(config, config.getFileTree());
	}

	public static boolean saveFileTree(FileTree fileTree) {
		return saveFileTree(null, fileTree);
	}
	public static boolean saveFileTree(Config c, FileTree fileTree) {
		try {
			fileTree.save();
			return true;
		} catch (Exception e) {
			FxAlert.showErrorDialog(c+"\n"+fileTree, "failed to save filetreee", e);
			return false;
		}
	}
	
	public static Window window() {
		return window;
	}
	public static HostServices hostServices() {
		return hostservice;
	}
	public static void set(Window window, HostServices hostservice) {
		if(hostservice != null)
			throw new IllegalStateException("calling second is not allowed");
		Utils.window = window;
		Utils.hostservice = hostservice;
	}

	public static void runAsync(Runnable runnable) {
		EXECUTOR_SERVICE.execute(runnable);
	}
	
	public static boolean saveInTempDirHideError(Writable w, Config config, String directory, String fileName) {
		try {
			saveInTempDir(w, config, directory, fileName);
			return true;
		} catch (IOException e) {
			errorHandler.accept(path(TEMP_DIR, directory, config.getName(), fileName), e);
			return false;
		}
	}

	public static Path saveInTempDir(Writable w, Config config, String directory, String fileName) throws IOException {
		Path p = path(TEMP_DIR, directory, config.getName(), fileName);
		save(p, w);
		return p;
	}
	private static Path path(Path root, String child1, String...child) {
		return root.resolve(Paths.get(child1, child));
	}

	private static void save(Path p, Writable w) throws IOException {
		Files.createDirectories(p.getParent());
		
		try(BufferedWriter os = Files.newBufferedWriter(p, WRITE, CREATE, TRUNCATE_EXISTING)) {
			w.write(os);
		}
	}
	public static void walk(Dir start, FileTreeWalker walker) {
		walk0(start, walker);
	}
	private static FileVisitResult walk0(Dir start, FileTreeWalker walker) {
		for (FileEntity f : start) {
			if(f.isDirectory() && asDir(f).isEmpty())
				continue;

			FileVisitResult result = f.isDirectory() ? walker.dir(asDir(f)) : walker.file(f);

			if(result == TERMINATE)
				return TERMINATE;
			if(result == SKIP_SIBLINGS)
				break;

			if(result != SKIP_SUBTREE && f.isDirectory() && walk0(asDir(f), walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}
	private static Dir asDir(FileEntity f) {
		return (Dir)f;
	}

	public static void setErrorHandler(BiConsumer<Object, Exception> errorHandler) {
		if(Utils.errorHandler != null)
			throw new IllegalStateException();
		Utils.errorHandler = errorHandler;
	}
}
