package sam.backup.manager.extra;

import static javafx.application.Platform.runLater;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.google.gson.Gson;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.file.FileTree;
import sam.console.ansi.ANSI;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.myutils.fileutils.FilesUtils;

public class Utils {
	public static final Path APP_DATA = Paths.get("app_data");
	public static final ExecutorService threadPool = Executors.newSingleThreadExecutor();

	private Utils() {}
	
	public static void run(Runnable r) {
		threadPool.execute(r);
	}
	public static void shutdown() {
		threadPool.shutdownNow();
	}

	public static Hyperlink hyperlink(Path path) {
		Hyperlink link = new Hyperlink(String.valueOf(path));
		if(path != null) {
			File f = path.toFile();
			if(!f.exists())
				link.setOnAction(e -> FxPopupShop.showHidePopup("file not found: \n"+f, 2000));
			else {
				if(f.isDirectory())
					link.setOnAction(e -> FilesUtils.openFileNoError(f));
				else
					link.setOnAction(e -> FilesUtils.openFileLocationInExplorerNoError(f));
			}
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
	private final static StringBuilder sb = new StringBuilder();
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
	private static void showErrorAndWait(Object text, Object header, Exception e) {
		AtomicBoolean b = new AtomicBoolean();
		runLater(() -> {
			FxAlert.showErrorDialog(text, header, e);
			b.set(true);
		});
		waitUntil(b);
	}
	public static  Path getConfigPath() {
		return APP_DATA.resolve("config.json");
	} 
	public static RootConfig readConfigJson() {
		try(Reader r = Files.newBufferedReader(getConfigPath());) {
			RootConfig root;
			root = new Gson().fromJson(r, RootConfig.class);
			return root;
		} catch (IOException e) {
			showErrorAndWait(Utils.getConfigPath().toAbsolutePath(), "Error reading config file", e);
			System.exit(0);
		}
		return null;
	}
	private static Path getTreePath(Config config) {
		return APP_DATA.resolve("trees/"+config.getSource().getFileName()+"-"+config.getSource().hashCode()+".filetree");
	}
	public static FileTree readFiletree(Config config) throws IOException {
		Path p = getTreePath(config); 

		if(Files.exists(p))
			return FileTree.read(p);

		return null;
	}
	public static void saveFiletree(Config config) throws IOException {
		Objects.requireNonNull(config.getFileTree(), "config does not have a filetree: "+config.getSource());

		Path p = getTreePath(config);
		Files.createDirectories(p.getParent());
		config.getFileTree().write(p);
		System.out.println(ANSI.yellow("file-tree saved: ")+p.getFileName());
	}
	public static Path getBackupLastPerformedPathTimeMapPath() {
		return APP_DATA.resolve("backup-last-performed.txt");
	}
	public static void saveBackupLastPerformedPathTimeMap(Map<String, Long>  map) {
		try {
			StringBuilder sb = new StringBuilder();
			map.forEach((s,t) -> sb.append(s).append('\t').append(t).append('\n'));
			Files.write(getBackupLastPerformedPathTimeMapPath(), sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Long> readBackupLastPerformedPathTimeMap() {
		HashMap<String, Long> map = new HashMap<>();
		try {
			Path p = getBackupLastPerformedPathTimeMapPath();
			if(Files.exists(p)) {
				Files.lines(p)
				.map(s -> s.split("\t"))
				.filter(s -> s.length == 2)
				.forEach(s -> {
					try {
						map.put(s[0], Long.parseLong(s[1]));
					} catch (NumberFormatException e) {}
				});
			}
		} catch (IOException e) {}
		return map;
	}

	public static Stage showStage(Parent ta) {
		Stage stg = new Stage();
		stg.initModality(Modality.WINDOW_MODAL);
		stg.initStyle(StageStyle.UTILITY);
		stg.initOwner(App.getStage());
		stg.setScene(new Scene(ta));
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
	
	public static boolean saveToFile(String text, Path expectedPath) {
		File file = selectFile(expectedPath.toFile(), "save filetree");
		
		if(file == null)
			return false;
		try {
			Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_16), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			return true;
		} catch (IOException e) {
			showErrorDialog("target: "+file, "failed to save" , e);
		}
		return false;
	}
	public static Logger getLogger(Class<?> cls) {
		return Logger.getLogger(cls.getSimpleName());
	} 
}
