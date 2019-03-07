package sam.backup.manager;

import java.nio.file.Path;

public interface AppConfig {
	public static enum ConfigName {
		SAVE_EXCLUDE_LIST, CONFIG_PATH_JSON
	}
	
	Path appDataDir(); 
	Path tempDir() ;
	Object getConfig(ConfigName name);
}
