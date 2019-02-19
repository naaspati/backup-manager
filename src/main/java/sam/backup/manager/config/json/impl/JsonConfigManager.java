package sam.backup.manager.config.json.impl;

import java.io.IOException;
import java.util.List;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.nopkg.Junk;

class JsonConfigManager implements ConfigManager {

	@Override
	public List<Config> getBackups() {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}

	@Override
	public List<Config> getLists() {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}

	public void load() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
