package sam.backup.manager.api;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.functions.IOExceptionConsumer;
import sam.io.serilizers.WriterImpl;

public interface IUtils {
	String bytesToString(long bytes) ;
	String millisToString(long millis) ;
	String durationToString(Duration d) ;
	double divide(long dividend, long divisor) ;
	String millsToTimeString(long d) ;
	Logger getLogger(Class<?> cls);
	String toString(int n);
	void write(Path path, boolean append, IOExceptionConsumer<WriterImpl> consumer) throws IOException;
	/**
	 * default action on Error
	 * @param path
	 * @param append
	 * @param consumer
	 */
	void writeHandled(Path path, boolean append, IOExceptionConsumer<WriterImpl> consumer);
	FileChannel fileChannel(Path path, boolean append) throws IOException;
	Path tempDir();
	Path tempDirFor(Config config);
}
