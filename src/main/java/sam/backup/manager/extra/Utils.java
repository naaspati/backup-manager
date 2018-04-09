package sam.backup.manager.extra;

import static javafx.application.Platform.runLater;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Formatter;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeReader;
import sam.backup.manager.file.FileTreeWriter;
import sam.fileutils.FilesUtils;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.tsv.TsvMap;

public class Utils {
	public static final Path APP_DATA_DIR = getAppDir();
	public static final Path TEMPS_DIR = APP_DATA_DIR.resolve("temps");
	public static final ExecutorService threadPool = Executors.newSingleThreadExecutor();
	private static final Logger LOGGER =  LogManager.getLogger(Utils.class);

	private Utils() {}
	
	static {
		ResourceBundle rb = ResourceBundle.getBundle("app-config");
		rb.keySet().forEach(s -> System.setProperty(s, rb.getString(s))); 
	}
	private static Path getAppDir() {
		String s = System.getenv("APP_DATA_DIR");
		if(s == null)
			s = System.getProperty("APP_DATA_DIR");
		if(s == null)
			s = System.getProperty("app_data_dir");
		if(s == null)
			s = System.getProperty("app.data.dir");
		if(s == null)
			s = "app_data";
		
		return Paths.get(s);
	}
	public static void run(Runnable r) {
		threadPool.execute(r);
	}
	public static void shutdown() {
		threadPool.shutdownNow();
	}

	public static Hyperlink hyperlink(Path path) {
		Hyperlink link = new Hyperlink(path == null ? "--" : path.toString());
		if(path == null) {
			link.setDisable(true);
			return link;
		}
		File f = path.toFile();
		if(!f.exists())
			link.setOnAction(e -> FxPopupShop.showHidePopup("file not found: \n"+f, 2000));
		else {
			if(f.isDirectory())
				link.setOnAction(e -> FilesUtils.openFileNoError(f));
			else
				link.setOnAction(e -> FilesUtils.openFileLocationInExplorerNoError(f));
		}
		link.setMinWidth(400);
		return link;
	}

	public static String bytesToString(long bytes) {
		if(bytes == 0)
			return "0";
		if(bytes < 1048576)
			return bytesToString(bytes, 1024) + "KB";
		if(bytes < 1073741824)
			return bytesToString(bytes, 1048576) + "MB";
		else
			return bytesToString(bytes, 1073741824) + "GB";

	}

	private static String bytesToString(long bytes, long divisor) {
		double d = divide(bytes, divisor);
		if(d == (int)d) return String.valueOf((int)d);
		else return String.valueOf(d);
	}
	public static String millisToString(long millis) {
		if(millis <= 0) return "N/A";
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
		if(divisor == 0 || dividend == 0)
			return 0;
		return (dividend*100/divisor)/100D;
	}
	public static String millsToTimeString(Long d) {
		return d == null || d <= 0 ? "--" : LocalDateTime.ofInstant(Instant.ofEpochMilli(d), ZoneOffset.of("+05:30")).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}
	private static void waitUntil(AtomicBoolean stopper) {
		while(!stopper.get()) {}
	}
	public static void showErrorAndWait(Object text, Object header, Exception e) {
		AtomicBoolean b = new AtomicBoolean();
		runLater(() -> {
			FxAlert.showErrorDialog(text, header, e);
			b.set(true);
		});
		waitUntil(b);
	}
	private static Path getTreePath(Config config, boolean isBackups) {
		return APP_DATA_DIR.resolve("trees/"+(isBackups ? "" : "lists/")+config.getSource().getFileName()+"-"+config.getSource().hashCode()+".filetree");
	}
	public static FileTree readFiletree(Config config, boolean isBackups) throws IOException {
		Path p = getTreePath(config, isBackups); 

		if(Files.exists(p))
			return new FileTreeReader().read(p, config);

		LOGGER.warn("treeFile file not found: {}", p);

		return null;
	}
	public static void saveFiletree(Config config, boolean isBackups) throws IOException {
		Objects.requireNonNull(config.getFileTree(), "config does not have a filetree: "+config.getSource());

		Path p = getTreePath(config, isBackups);
		Files.createDirectories(p.getParent());
		new FileTreeWriter().write(p, config.getFileTree());
		LOGGER.info("file-tree saved: {}", p.getFileName());
	}
	public static Path getBackupLastPerformedPathTimeMapPath() {
		return APP_DATA_DIR.resolve("backup-last-performed.tsv");
	}
	public static void saveBackupLastPerformedPathTimeMap(Map<String, Long>  map) {
		try {
			((TsvMap<String, Long>)map).save(getBackupLastPerformedPathTimeMapPath());
		} catch (IOException e) {
			LOGGER.warn("failed to save: {}", getBackupLastPerformedPathTimeMapPath(), e);
		}
	}
	private static TsvMap<String, Long> backupLastPerformed ;
	private static volatile boolean backupLastPerformedModified = false;
	
	public static void readBackupLastPerformedPathTimeMap() {
		if(backupLastPerformed != null)
			return;
		Path p = getBackupLastPerformedPathTimeMapPath();
		try {
			backupLastPerformed =  TsvMap.parse(p, Long::parseLong, false);
		} catch (IOException e) {
			LOGGER.warn("failed to read: {}", p, e);
			backupLastPerformed = new TsvMap<>();
		}
	}

	public static Long getBackupLastPerformed(String key) {
		readBackupLastPerformedPathTimeMap();
		return backupLastPerformed.get(key);
	}
	public static void putBackupLastPerformed(String key, long time) {
		readBackupLastPerformedPathTimeMap();
		backupLastPerformed.put(key, time);
		backupLastPerformedModified = true;
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

		if(expectedFile != null) {
			File parent = expectedFile.getParentFile();
			boolean b = parent.exists();
			if(!b) {
				b = parent.mkdirs();
				if(!b)
					FxPopupShop.showHidePopup("failed to create\n"+parent, 1500);	
			}
			fc.setInitialDirectory(b ? parent : new File(System.getProperty("user.home")));
			fc.setInitialFileName(expectedFile.getName());
		}

		fc.setTitle(title);
		return fc.showSaveDialog(App.getStage());
	}

	public static boolean saveToFile2(CharSequence text, Path expectedPath) {
		File file = selectFile(expectedPath == null ? null : expectedPath.toFile(), "save filetree");

		if(file == null)
			return false;
		try {
			write(file.toPath(), text);
			return true;
		} catch (IOException e) {
			showErrorDialog("target: "+file, "failed to save" , e);
		}
		return false;
	}

	private static final StringBuilder sb2 = new StringBuilder();
	private static final Formatter fm = new Formatter(sb2);

	public static String format(String format, Object...args) {
		synchronized(sb2) {
			sb2.setLength(0);
			fm.format(format, args);
			return sb2.toString();
		}
	}
	public static Path subpath(Path child, Path parent) {
		if(parent == null)
			return child;

		return child == null || 
				child.getNameCount() < parent.getNameCount() || 
				!child.startsWith(parent) || 
				child.equals(parent) ? child : child.subpath(parent.getNameCount(), child.getNameCount()) ;
	}

	public static String hashedName(Path p, String ext) {
		return p.getFileName()+"-"+p.hashCode()+ext;
	}

	public static void write(Path path, CharSequence data) throws IOException {
		FilesUtils.write(path, data, StandardCharsets.UTF_8,false, CodingErrorAction.REPLACE,CodingErrorAction.REPLACE);
	}

	public static void stop() throws InterruptedException {
		if(backupLastPerformedModified)
			Utils.saveBackupLastPerformedPathTimeMap(backupLastPerformed);
		
		threadPool.shutdownNow();
		System.out.println("waiting thread to die");
		threadPool.awaitTermination(2, TimeUnit.SECONDS);
	}
}
