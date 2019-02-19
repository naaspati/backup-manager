package sam.backup.manager.config.api;

import java.io.IOException;

import sam.nopkg.EnsureSingleton;

public interface ConfigManagerProvider {
	public static final EnsureSingleton singleton = new EnsureSingleton();
	
	void load() throws IOException;
	ConfigManager get();
}