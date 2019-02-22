package sam.backup.manager.config.json.impl;

import java.io.IOException;
import java.util.List;

import javax.inject.Singleton;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;

@Singleton
class JsonConfigManager implements ConfigManager {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	
	{
		singleton.init();
	}
	
	boolean loaded = false;
	private void ensureLoaded() {
		if(!loaded)
			throw new IllegalStateException("no loaded");
	}

	@Override
	public List<Config> getBackups() {
		ensureLoaded();
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}

	@Override
	public List<Config> getLists() {
		ensureLoaded();
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}

	@Override
	public void load() throws IOException {
		if(loaded)
			return;
		
		// TODO Auto-generated method stub
		
		loaded = true;
	}

	@Override
	public Long getBackupLastPerformed(ConfigType type, Config config) {
		ensureLoaded();
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putBackupLastPerformed(ConfigType type, Config config, long time) {
		ensureLoaded();
		
		// TODO Auto-generated method stub
		
	}

}
