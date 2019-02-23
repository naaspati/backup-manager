package sam.backup.manager;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.Writable;

public final class Utils {
	private static IUtils utils;

	static void setUtils(IUtils utils) {
		Utils.utils = utils;
	}

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
	public static Logger getLogger(@SuppressWarnings("rawtypes") Class cls) {
		if(utils == null)
			return LogManager.getLogger(cls);
		else
			return utils.getLogger(cls);
	}

	public static boolean saveInTempDirHideError(Writable w, Config config, String directory, String fileName) {
		return utils.saveInTempDirHideError(w, config, directory, fileName);
	}

	public static Path saveInTempDir(Writable w, Config config, String directory, String fileName) throws IOException {
		return utils.saveInTempDir(w, config, directory, fileName);
	}
	public static void setTextNoError(Path target, CharSequence content, String errorMessage) {
		utils.setTextNoError(target, content, errorMessage);
	}
}
