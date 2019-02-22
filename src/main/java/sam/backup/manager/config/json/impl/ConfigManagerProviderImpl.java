package sam.backup.manager.config.json.impl;

import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerProvider;

public class ConfigManagerProviderImpl implements ConfigManagerProvider {
	@Override
	public ConfigManager get() throws Exception {
		return new JsonConfigManager();
	}
}
