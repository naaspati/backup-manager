package sam.backup.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.PathWrap;
import sam.backup.manager.extra.Writable;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileTreeWalker;

public final class Utils {
	public static  final IUtils utils = SingleLoader.load(IUtils.class, UtilsImpl.class);
	public static  final IFxUtils fx = SingleLoader.load(IFxUtils.class, IFxUtilsImpl.class);

	public static  final Path APP_DATA = utils.appDataDir();
	public static  final Path TEMP_DIR = utils.tempDir();
	public static  final boolean SAVE_EXCLUDE_LIST = utils.isSaveExcludeList();

	private Utils() { }
	static void init() { }
	
	public static Path appDataDir() {
		return utils.appDataDir();
	}
	public static boolean isSaveExcludeList() {
		return utils.isSaveExcludeList();
	}
	public static Path tempDir() {
		return utils.tempDir();
	}
	public static String bytesToString(long bytes) {
		return utils.bytesToString(bytes);
	}
	public static String millisToString(long millis) {
		return utils.millisToString(millis);
	}
	public static String durationToString(Duration d) {
		return utils.durationToString(d);
	}
	public static double divide(long dividend, long divisor) {
		return utils.divide(dividend, divisor);
	}
	public static String millsToTimeString(Long d) {
		return utils.millsToTimeString(d);
	}
	public static Long getBackupLastPerformed(String key) {
		return utils.getBackupLastPerformed(key);
	}
	public static void putBackupLastPerformed(String key, long time) {
		utils.putBackupLastPerformed(key, time);
	}
	public static Stage showStage(Window parent, Parent content) {
		return fx.showStage(parent, content);
	}
	public static void showErrorDialog(Object text, String header, Exception error) {
		fx.showErrorDialog(text, header, error);
	}
	public static FileChooser selectFile(File expectedDir, String expectedName, String title) {
		return fx.selectFile(expectedDir, expectedName, title);
	}
	public static String hashedName(Path p, String ext) {
		return utils.hashedName(p, ext);
	}
	public static void write(Path path, CharSequence data) throws IOException {
		utils.write(path, data);
	}
	public static void writeInTempDir0(Config config, String prefix, String suffix, CharSequence data, Logger logger)
			throws IOException {
		utils.writeInTempDir0(config, prefix, suffix, data, logger);
	}
	public static void writeInTempDir(Config config, String prefix, String suffix, CharSequence data, Logger logger) {
		utils.writeInTempDir(config, prefix, suffix, data, logger);
	}
	static void stop() {
		try {
			utils.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fx.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void stylesheet(Parent node) {
		fx.stylesheet(node);
	}
	public static Node hyperlink(PathWrap wrap) {
		return fx.hyperlink(wrap);
	}
	public static Node hyperlink(List<PathWrap> wraps) {
		return fx.hyperlink(wraps);
	}
	public static <T> T either(T t1, T t2, T defaultValue) {
		if(t1 == null && t2 == null)
			return defaultValue;
		return t1 != null ? t1 : t2;
	}
	
	public static Node button(String tooltip, String icon, EventHandler<ActionEvent> action) {
		return fx.button(tooltip, icon, action);
	}
	public static Logger getLogger(Class<?> cls) {
		return utils.getLogger(cls);
	}
	public static void runAsync(Task runnable) {
		fx.runAsync(runnable);
	}
	public static boolean saveInTempDirHideError(Writable w, Config config, String directory, String fileName) {
		return utils.saveInTempDirHideError(w, config, directory, fileName);
	}
	public static Path saveInTempDir(Writable w, Config config, String directory, String fileName) throws IOException {
		return utils.saveInTempDir(w, config, directory, fileName);
	}
	public static void walk(Dir start, FileTreeWalker walker) {
		utils.walk(start, walker);
	}
	static void setErrorHandler(BiConsumer<Object, Exception> errorHandler) {
		utils.setErrorHandler(errorHandler);
		fx.setErrorHandler(errorHandler);
	}
	public static void fx(Runnable runnable) {
		fx.fx(runnable);
	}
}
