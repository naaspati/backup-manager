package sam.backup.manager.config.api;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public interface ConfigManager {
	List<Config> getBackups();
	List<Config> getLists();
} 