package sam.backup.manager.extra;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import sam.fileutils.FilesUtils;
import sam.myutils.MyUtilsThread;


public final class Files2 {
	private static final boolean DRY_RUN;
	private static final Path TEMP_DIR;
	private static final Logger LOGGER;

	private Files2() {}

	static {
		DRY_RUN = Optional.ofNullable(System.getenv("DRY_RUN")).map(s -> s.trim().equalsIgnoreCase("true")).orElse(false);
		if(DRY_RUN) {
			try {
				TEMP_DIR = Files.createTempDirectory("backup_maneger_dry_run");
			} catch (IOException e) {
				throw new RuntimeException("failed to create temp_dir (backup_maneger_dry_run)", e);
			}
			LOGGER = LoggerFactory.getLogger("DRY_RUN");
			MyUtilsThread.addShutdownHook(() -> {
				try {
					FilesUtils.deleteDir(TEMP_DIR);
				} catch (IOException e) {
					LOGGER.warn("failed to delete temp_dir: {}", TEMP_DIR, e);
				}
			});
			LOGGER.debug("DRY_RUN enabled");
		} else {
			TEMP_DIR = null;
			LOGGER = null;
		}
	}

	public static void createDirectories(Path p) throws IOException {
		if(DRY_RUN) {
			Path p2 = TEMP_DIR.resolve(String.valueOf(p.hashCode()));
			Files.createDirectories(p2);
			LOGGER.debug("create temp dir for: {} -> {}", p, p2);
		}
		else
			Files.createDirectories(p);
	}

	public static void move(Path src, Path target, StandardCopyOption...options) throws IOException {
		if(DRY_RUN)
			LOGGER.debug("file move: {} -> {}", src, target);
		else
			Files.move(src, target, options);
	}

	public static void delete(Path path) throws IOException {
		if(DRY_RUN)
			LOGGER.debug("file delete: {}", path);
		else
			Files.delete(path);
	}

	public static InputStream newInputStream(Path path, StandardOpenOption...options) throws IOException {
		if(DRY_RUN) {
			Path p2 = Files.createTempFile(path.getFileName().toString(), "");
			LOGGER.debug("create input stream: {}", path);
			return Files.newInputStream(p2, options);
		}
		else
			return Files.newInputStream(path, options);
	}

	public static OutputStream newOutputStream(Path path, StandardOpenOption...options) throws IOException {
		if(DRY_RUN) {
			Path p2 = Files.createTempFile(path.getFileName().toString(), "");
			LOGGER.debug("create Output stream: {}", path);
			return Files.newOutputStream(p2, options);
		}
		else
			return Files.newOutputStream(path, options);
	}

	public static boolean delete(File file) {
		if(DRY_RUN) {
			LOGGER.debug("file delete: {}", file);
			return true;
		}
		else
			return file.delete();
	}
}
