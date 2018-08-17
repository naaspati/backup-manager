package sam.backup.manager.config;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

import sam.backup.manager.extra.Utils;

public class ConfigReader {
	public RootConfig read(Path path) {
		try(Reader r = Files.newBufferedReader(path);) {
			return new Gson().fromJson(r, RootConfig.class);
		} catch (Exception e) {
			Utils.showErrorAndWait(path, "failed to parse", e);
			System.exit(0);
		}
		return null;
	}
}
