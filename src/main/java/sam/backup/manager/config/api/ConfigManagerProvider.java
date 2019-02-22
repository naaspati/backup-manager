package sam.backup.manager.config.api;

public interface ConfigManagerProvider {
	ConfigManager get() throws Exception ;
}