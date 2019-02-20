package sam.backup.manager.config.api;

import java.util.List;

import javax.inject.Singleton;

import org.codejargon.feather.Provides;

@Singleton
public interface ConfigManager {
	@Provides
	@Backups
	List<Config> getBackups();
	
	@Provides
	@Lists
	List<Config> getLists();
} 