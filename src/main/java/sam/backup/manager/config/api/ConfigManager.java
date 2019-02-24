package sam.backup.manager.config.api;

import java.nio.file.Path;
import java.util.Collection;

import javax.inject.Singleton;

import sam.backup.manager.Injector;
import sam.nopkg.Junk;

@Singleton
public interface ConfigManager {
	Collection<Config> get(ConfigType type);
	void load(Path path, Injector injector) throws Exception; 
	
	Long getBackupLastPerformed(ConfigType type, Config config) ;
	void putBackupLastPerformed(ConfigType type, Config config, long time);
	
	public static <T> T either(T t1, T t2, T defaultValue) {
		if(t1 == null && t2 == null)
			return defaultValue;
		return t1 != null ? t1 : t2;
	}
	default String key(ConfigType type, Config config) {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
} 