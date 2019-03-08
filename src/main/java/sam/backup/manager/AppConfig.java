package sam.backup.manager;

import java.nio.file.Path;

public interface AppConfig {
	Path appDataDir(); 
	Path tempDir() ;
	String getConfig(String name);
	Path backupDrive();
}
