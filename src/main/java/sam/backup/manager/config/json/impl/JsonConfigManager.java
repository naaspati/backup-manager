package sam.backup.manager.config.json.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.Stoppable;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.nopkg.SavedResource;

@Singleton
class JsonConfigManager implements ConfigManager, Stoppable {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	private final Logger logger = Utils.getLogger(JsonConfigManager.class);
	private Runnable backupLastPerformed_mod;
	private SavedResource<Map<String, Long>> backupLastPerformed;
	
	
	public JsonConfigManager() {
		singleton.init();

		// TODO Auto-generated method stub
		
		// load configs
		
		backupLastPerformed = create();
		// TODO Auto-generated constructor stub
	}

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

	private SavedResource<Map<String, Long>> create() {
		return new SavedResource<Map<String,Long>>() {
			private final Path path = sam.backup.manager.Utils.appDataDir().resolve("backup-last-performed.dat");

			{
				backupLastPerformed_mod = this::increamentMod;
			}

			@Override
			public void write(Map<String, Long> data) {
				try {
					logger.debug("write {}", path);
					ObjectWriter.writeMap(path, data, DataOutputStream::writeUTF, DataOutputStream::writeLong);
				} catch (IOException e) {
					logger.warn("failed to save: {}", path, e);
				}
			}

			@Override
			public Map<String, Long> read() {
				if(Files.notExists(path))
					return new HashMap<>();

				try {
					logger.debug("READ {}", path);
					return ObjectReader.readMap(path, d -> d.readUTF(), DataInputStream::readLong);
				} catch (IOException e) {
					logger.warn("failed to read: {}", path, e);
					return new HashMap<>();
				}
			}
		};
	}

	@Override
	public Long getBackupLastPerformed(ConfigType type, Config config) {
		
		return backupLastPerformed.get().get(key(type, config));
	}
	private String key(ConfigType type, Config config) {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}

	@Override
	public void putBackupLastPerformed(ConfigType type, Config config, long time) {
		
		backupLastPerformed.get().put(key(type, config), time);
		backupLastPerformed_mod.run();
	}

	@Override
	public void stop() throws Exception {
		backupLastPerformed.close();
	}
}
