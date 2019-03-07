package sam.backup.manager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.Writable;

public interface IUtils {
	void setAppConfig(AppConfig config);
	
	String bytesToString(long bytes) ;
	String millisToString(long millis) ;
	String durationToString(Duration d) ;
	double divide(long dividend, long divisor) ;
	String millsToTimeString(long d) ;
	String hashedName(Path p, String ext) ;
	void write(Path path, CharSequence data) throws IOException ;
	void writeInTempDir0(Config config, String prefix, String suffix, CharSequence data, Logger logger) throws IOException ;
	void writeInTempDir(Config config, String prefix, String suffix, CharSequence data, Logger logger) ;
	
	boolean saveInTempDirHideError(Writable w, Config config, String directory, String fileName) ;
	Path saveInTempDir(Writable w, Config config, String directory, String fileName) throws IOException ;
	/**
	 * set text to a file, error is handled by default handler
	 * @param file
	 * @param ft
	 */
	void setTextNoError(Path target, CharSequence content, String errorMessage);
	Logger getLogger(Class<?> cls);
}
