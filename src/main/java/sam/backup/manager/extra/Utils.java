package sam.backup.manager.extra;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.io.Reader;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.Main;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.file.FileTree;
import sam.fx.alert.FxAlert;
import sam.myutils.fileutils.FilesUtils;

public class Utils {
	private Utils() {}

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

	private final static StringBuilder sb = new StringBuilder();
	public static String millisToString(long millis) {
		if(millis == 0) return "N/A";
		return durationToString(Duration.ofMillis(millis));
	}
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
		return d == null || d == 0 ? "--" : LocalDateTime.ofInstant(Instant.ofEpochMilli(d), ZoneOffset.of("+05:30")).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
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
		return Paths.get("config.json");
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

	public static  Path getPathFiletreeMapPath() {
		return Paths.get("path-filetree.dat");
	} 
	public static ConcurrentHashMap<String, FileTree> readPathFiletreeMap() {
		Path p = getPathFiletreeMapPath();
		if(Files.notExists(p)) {
			AtomicBoolean proceed = new AtomicBoolean();
			AtomicBoolean waitStop = new AtomicBoolean();
			Platform.runLater(() -> {
				proceed.set(FxAlert.showConfirmDialog("expected path: "+getPathFiletreeMapPath().toAbsolutePath(), "filetree backup not found\nProceed Anyways?"));
				waitStop.set(true);
			});
			waitUntil(waitStop);
			if(proceed.get())
				return new ConcurrentHashMap<>();
			else
				System.exit(0);
		} else {
			try {
				return FilesUtils.readObjectFromFile(p);
			} catch (IOException | ClassNotFoundException e) {
				showErrorAndWait(p, "Failed to read file tree backup", e);
				System.exit(0);
			}
		}
		return null;
	}

	public static void savePathFiletreeMap(ConcurrentHashMap<String, FileTree> map) {
		Path p = getPathFiletreeMapPath();
		try {
			FilesUtils.writeObjectToFile(map, p);
			System.out.println("modified: "+p);
		} catch (IOException e) {
			FxAlert.showErrorDialog(null, "failed to write: "+p, e);
		}
	}
	public static Path getBackupLastPerformedPathTimeMapPath() {
		return Paths.get("backup-last-performed.txt");
	}
	
	public static void saveBackupLastPerformedPathTimeMap(Map<String, Long>  map) {
		try {
			StringBuilder sb = new StringBuilder();
			map.forEach((s,t) -> sb.append(s).append('\t').append(t).append('\n'));
			Files.write(getBackupLastPerformedPathTimeMapPath(), sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {}
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
	public static void saveExcludes(Map<Path, List<Path>> map) {
		Path p = Paths.get("excluded-files.txt"); 
		try {
			StringBuilder sb = new StringBuilder();

			sb.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))).append('\n').append('\n');
			map.forEach((s,t) -> {
				if(t.isEmpty())
					return;
				sb.append(s).append('\n');
				t.forEach(path -> sb.append("   ").append(path.startsWith(s) ? path.subpath(s.getNameCount(),path.getNameCount()) : path).append('\n'));
				sb.append('\n');
			});

			if(Files.exists(p)) {
				char[] c = new char[30];
				Arrays.fill(c, '-');
				sb.append('\n').append(c).append('\n');
				sb.append(new String(Files.readAllBytes(p)));
			}

			Files.write(p, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			System.out.println("modified: excluded-files.txt");
		} catch (IOException e) {
			System.out.println("failed to save: excluded-files.txt");
			e.printStackTrace();
		}
	}

	public static Stage showStage(Parent ta) {
		Stage stg = new Stage();
		stg.initModality(Modality.WINDOW_MODAL);
		stg.initStyle(StageStyle.UTILITY);
		stg.initOwner(Main.getStage());
		stg.setScene(new Scene(ta));
		stg.getScene().getStylesheets().setAll(Main.getStage().getScene().getStylesheets());
		stg.setWidth(300);
		stg.setHeight(400);
		runLater(stg::show);
		
		return stg;
	}
}
