package sam.backup.manager;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.api.IUtils;
import sam.backup.manager.config.api.Config;
import sam.functions.IOExceptionConsumer;
import sam.io.serilizers.WriterImpl;

public final class Utils {
	private static IUtils utils;

	static void setUtils(IUtils utils) {
		Utils.utils = utils;
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
	public static String millsToTimeString(long d) {
		return utils.millsToTimeString(d);
	}
	
	public static Logger getLogger(@SuppressWarnings("rawtypes") Class cls) {
		if(utils == null)
			return LogManager.getLogger(cls);
		else
			return utils.getLogger(cls);
	}

	static int number(Path path) {
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
	public static String toString(int n) {
		return utils.toString(n);
	}
	public static void write(Path path, boolean append, IOExceptionConsumer<WriterImpl> consumer) throws IOException {
		utils.write(path, append, consumer);
	}
	public static void writeHandled(Path path, boolean append, IOExceptionConsumer<WriterImpl> consumer) {
		utils.writeHandled(path, append, consumer);
	}
	public static FileChannel fileChannel(Path path, boolean append) throws IOException {
		return utils.fileChannel(path, append);
	}
	public static Path tempDir() {
		return utils.tempDir();
	}
	public static Path tempDirFor(Config config) {
		return utils.tempDirFor(config);
	}
}
