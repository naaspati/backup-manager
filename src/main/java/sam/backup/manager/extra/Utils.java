package sam.backup.manager.extra;

import static javafx.application.Platform.runLater;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.CodingErrorAction;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeReader;
import sam.backup.manager.file.FileTreeWriter;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxHyperlink;
import sam.fx.popup.FxPopupShop;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.io.serilizers.StringWriter2;
import sam.myutils.MyUtilsPath;
import sam.nopkg.LazyLoadedData;
import sam.reference.WeakAndLazy;

public final class Utils {
	private static final Logger LOGGER;
	public static final Path APP_DATA = Paths.get("app_data");
	public static final Path TEMP_DIR;
	private static final Supplier<String> counter;

	public static ExecutorService threadPool0;

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
			int n = number(TEMP_DIR);

			@Override
			public String get() {
				return (n++)+" - ";
			}
		};

		LOGGER = Utils.getLogger(Utils.class);
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LOGGER.error("thread: {}", thread.getName(), exception));
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

	private static ExecutorService threadPool() {
		if (threadPool0 == null)
			threadPool0 = Executors.newSingleThreadExecutor();

		return threadPool0;
	}

	public static void run(Runnable r) {
		threadPool().execute(r);
	}

	public static void shutdown() {
		threadPool().shutdownNow();
	}

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

	private static Path getTreePath(Config config, TreeType treeType) {
		return APP_DATA.resolve("trees/"+treeType+"-"+config.getName()+".filetree");
	}
	public static FileTree readFiletree(Config config, TreeType treeType) throws IOException {
		Path p = getTreePath(config, treeType);

		try {
			if (Files.exists(p))
				return new FileTreeReader().read(p, config);
		} catch (IOException e) {
			throw new IOException(e.getMessage()+" ("+p+")", e);
		}

		LOGGER.warn("treeFile file not found: {}", p);
		return null;
	}

	public static void saveFiletree0(Config config, TreeType treeType) throws IOException {
		Objects.requireNonNull(config.getFileTree(), "config does not have a filetree: " + config.getSource());

		Path p = getTreePath(config, treeType);

		try {
			Files.createDirectories(p.getParent());
			new FileTreeWriter().write(p, config.getFileTree());
			LOGGER.info("file-tree saved: {}", p.getFileName());
		} catch (IOException e) {
			throw new IOException(e.getMessage()+" ("+p+")", e);
		}
	}

	public static void saveFiletree(Config config, TreeType treeType) {
		try {
			saveFiletree0(config, treeType);
		} catch (IOException e) {
			FxAlert.showErrorDialog(null, "failed to save filetree", e);
		}
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
		stg.initOwner(App.getStage());
		stg.setScene(new Scene(content));
		stg.getScene().getStylesheets().setAll(App.getStage().getScene().getStylesheets());
		stg.setWidth(300);
		stg.setHeight(400);
		runLater(stg::show);

		return stg;
	}

	public static void showErrorDialog(Object text, String header, Exception error) {
		runLater(() -> FxAlert.showErrorDialog(text, header, error));
	}

	public static File selectFile(File expectedFile, String title) {
		FileChooser fc = new FileChooser();

		if (expectedFile != null) {
			File parent = expectedFile.getParentFile();
			boolean b = parent.exists();
			if (!b) {
				b = parent.mkdirs();
				if (!b)
					FxPopupShop.showHidePopup("failed to create\n" + parent, 1500);
			}
			fc.setInitialDirectory(b ? parent : new File(System.getProperty("user.home")));
			fc.setInitialFileName(expectedFile.getName());
		}

		fc.setTitle(title);
		return fc.showSaveDialog(App.getStage());
	}

	public static boolean saveToFile2(CharSequence text, Path expectedPath) {
		File file = selectFile(expectedPath == null ? null : expectedPath.toFile(), "save filetree");

		if (file == null)
			return false;
		try {
			write(file.toPath(), text);
			return true;
		} catch (IOException e) {
			showErrorDialog("target: " + file, "failed to save", e);
		}
		return false;
	}

	public static String hashedName(Path p, String ext) {
		return p.getFileName() + "-" + p.hashCode() + ext;
	}

	public static void write(Path path, CharSequence data) throws IOException {
		Files.createDirectories(path.getParent());
		StringWriter2.writer()
		.onMalformedInput(CodingErrorAction.REPLACE)
		.onUnmappableCharacter(CodingErrorAction.REPLACE)
		.target(path, false)
		.write(data);
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

		threadPool().shutdownNow();
		System.out.println("waiting thread to die");
		threadPool().awaitTermination(2, TimeUnit.SECONDS);
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

	public static Node hyperlink(Path path, String alternative) {
		if(path != null) {
			Hyperlink hyperlink = FxHyperlink.of(path);
			hyperlink.setMinWidth(400);
			return hyperlink;
		} else {
			Text t = new Text(alternative);
			t.setDisable(true);
			return t;
		}
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
		return LoggerFactory.getLogger(cls.getSimpleName());
	}
}
