package sam.backup.manager.config.api;

import sam.backup.manager.AppConfig;

public interface ConfigManagerFactory {
	ConfigManager newInstance(AppConfig config) throws Exception;
}
