package sam.backup.manager.config.json.impl;

import java.io.IOException;

import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerProvider;

public class ConfigManagerProviderImpl implements ConfigManagerProvider {
	private static final JsonConfigManager configs = new JsonConfigManager();
	
	@Override
	public void load() throws IOException {
		singleton.init();
		configs.load();
	}
	@Override
	public ConfigManager get() {
		return configs;
	}
}
