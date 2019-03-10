package sam.backup.manager.config.json.impl;

import sam.backup.manager.AppConfig;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerFactory;

public class JsonConfigManagerFactory implements ConfigManagerFactory {
	@Override
	public ConfigManager newInstance(AppConfig config) throws Exception {
		return new JsonConfigManager(config);
	}
}
