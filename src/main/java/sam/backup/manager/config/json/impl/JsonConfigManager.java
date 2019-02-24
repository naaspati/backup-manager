package sam.backup.manager.config.json.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import sam.backup.manager.Stoppable;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.ConfigImpl;
import sam.backup.manager.config.impl.FilterImpl;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.FileTree;
import sam.myutils.Checker;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.nopkg.SavedResource;
import sam.nopkg.TsvMapTemp;

@Singleton
public class JsonConfigManager implements ConfigManager, Stoppable {
	public static final String  BACKUPS = "backups";
	public static final String  LISTS = "lists";
	public static final String  NAME = "name";
	public static final String  SOURCE = "source";
	public static final String  TARGET = "target";
	public static final String  BACKUP_CONFIG = "backupConfig";
	public static final String  WALK_CONFIG = "walkConfig";
	public static final String  EXCLUDES = "excludes";
	public static final String  TARGET_EXCLUDES  = "targetExcludes"; 
	public static final String  DISABLE = "disable";
	public static final String  ZIP_IF = "zipIf";
	
	
	private static final EnsureSingleton singleton = new EnsureSingleton();
	private final Logger logger = Utils.getLogger(JsonConfigManager.class);
	private Runnable backupLastPerformed_mod;
	private SavedResource<TsvMapTemp> backupLastPerformed;
	private boolean loaded = false;
	
	private Map<String, String> variables; 
	private List<ConfigImpl> backups, lists;
	
	public JsonConfigManager() {
		singleton.init();
	}
	private void ensureLoaded() {
		if(!loaded)
			throw new IllegalStateException("not loaded");
	}
	
	@Override
	public void load(Path jsonPath) throws Exception {
		if(loaded)
			return;
		
		loaded = true;
		
		backupLastPerformed =  new SavedResource<TsvMapTemp>() {
			private final Path path = jsonPath.resolveSibling("backup-last-performed.tsv");
			{
				backupLastPerformed_mod = this::increamentMod;
			}

			@Override
			public void write(TsvMapTemp data) {
				data.values().removeIf(Checker::isEmptyTrimmed);
				try {
					if(data.isEmpty())
						Files.deleteIfExists(path);
					else
						data.save(path);
				} catch (Exception e) {
					logger.warn("failed to write: {}", path, e);
				}
					
			}

			@Override
			public TsvMapTemp read() {
				if(Files.exists(path)) {
					try {
						logger.debug("READ {}", path);
						return new TsvMapTemp(path);
					} catch (IOException e) {
						logger.warn("failed to read: {}", path, e);
					}
				}
				return new TsvMapTemp();
			}
		};
	
		
		try(BufferedReader reader = Files.newBufferedReader(jsonPath)) {
			JSONObject json = new JSONObject(new JSONTokener(reader));	
			
			JSONObject vars = json.optJSONObject("variables");
			if(vars != null) {
				variables = new HashMap<>();
				vars.keySet()
				.forEach(s -> variables.put(s, vars.getString(s)));
			}
			
			if(Checker.isEmpty(variables))
				variables = Collections.emptyMap();
			
			Config temp = (Config) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Config.class}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return null;
				}
			});
			
			if(!json.has(NAME))
				json.put(NAME, "root-json-config");
				
			ConfigImpl root = config(temp, json); 
			
			backups = parseArray(root, json, BACKUPS);
			lists = parseArray(root, json, LISTS);
		}
	}

	private List<ConfigImpl> parseArray(ConfigImpl root, JSONObject json, String key) {
		Object obj = json.get(key);
		if(obj == null)
			return Collections.emptyList();
		if(!(obj instanceof JSONArray))
			throw new JSONException("expected type: JSONArray, found: "+obj.getClass()+", for key: "+key);

		JSONArray array = (JSONArray) obj;
		
		if(array.isEmpty())
			return Collections.emptyList();
		
		ConfigImpl config[] = new ConfigImpl[array.length()]; 
		
		for (int i = 0; i < config.length; i++) 
			config[i] = config(root, array.getJSONObject(i));
		return Collections.unmodifiableList(Arrays.asList(config));
	}
	public Path resolve(String s) {
		return Junk.notYetImplemented();
		/* FIXME
		 * {
						if(s.charAt(0) == '\\' || s.charAt(0) == '/')
							return config.getSource().resolve(s.substring(1));
						if(s.contains("%source%"))
							s = s.replace("%source%", config.getSource().toString());
						if(s.contains("%target%") && config.getTarget() != null)
							s = s.replace("%target%", config.getTarget().toString());
						return Paths.get(s);
					}
		 */
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<Config> get(ConfigType type) {
		ensureLoaded();
		Objects.requireNonNull(type);
		return (List)(type == ConfigType.LIST ? lists : backups);
	}
	
	private static final String DEFAULT_STRING = new String();
	private static final Long ZERO = Long.valueOf(0);

	@Override
	public Long getBackupLastPerformed(ConfigType type, Config config) {
		ensureLoaded();
		
		String s = backupLastPerformed.get().getOrDefault(key(type, config), DEFAULT_STRING);
		if(s == null || DEFAULT_STRING.equals(s))
			return ZERO;
		
		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			logger.error("failed to convert to long: {}", s, e);
		}
		return ZERO;
	}

	@Override
	public void putBackupLastPerformed(ConfigType type, Config config, long time) {
		ensureLoaded();
		
		backupLastPerformed.get().put(key(type, config), time == 0 ? DEFAULT_STRING : Long.toString(time));
		backupLastPerformed_mod.run();
	}

	@Override
	public void stop() throws Exception {
		backupLastPerformed.close();
	}
	public static List<String> getList(Object obj, boolean unmodifiable) {
		if(obj == null)
			return Collections.emptyList();
		if(obj.getClass() == String.class)
			return Collections.singletonList((String)obj);
		if(obj instanceof JSONArray) {
			JSONArray array = ((JSONArray) obj);
			if(array.isEmpty())
				return Collections.emptyList();
			if(array.length() == 1)
				return Collections.singletonList(array.getString(0));

			String[] str = new String[array.length()];
			for (int i = 0; i < array.length(); i++) 
				str[i] = array.getString(i);

			List<String> list = Arrays.asList(str);

			return unmodifiable ? Collections.unmodifiableList(list) : list;
		}
		throw new IllegalArgumentException("bad type: "+obj);
	}

	public static FilterImpl getFilter(Object obj, String jsonKey) {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
	
	private ConfigImpl config(Config global, JSONObject json) {
		try {
			/* FIXME configure it to create to List<FiletreeMeta>
			 *  
			this.source = getList(json.opt(SOURCE), true);
			if(this.source.isEmpty())
				throw new JSONException("source not found in json");
			this.target = json.getString(TARGET);
			 */
			
			String name = json.getString(NAME);
			List<FileTreeMeta> ftms = Junk.notYetImplemented(); //FIXME
			
			Boolean disabled = json.optBoolean(DISABLE, false);
			FilterImpl zip = getFilter(json.opt(ZIP_IF), ZIP_IF);
			FilterImpl excludes = getFilter(json.opt(EXCLUDES), EXCLUDES);
			FilterImpl targetExcludes = getFilter(json.opt(TARGET_EXCLUDES), TARGET_EXCLUDES);
			BackupConfigImpl backupConfig = set(json.get(BACKUP_CONFIG), new BackupConfigImpl((BackupConfigImpl) global.getBackupConfig()));
			WalkConfigImpl walkConfig = set(json.get(WALK_CONFIG), new WalkConfigImpl((WalkConfigImpl) global.getWalkConfig()));
			
			return new ConfigImpl(name, ftms, disabled, zip, excludes, targetExcludes, backupConfig, walkConfig);
		} catch (Exception e) {
			throw new JSONException(e.getMessage()+"\n"+json, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <E> E set(Object jsonObj, Settable settable) {
		if(jsonObj != null) {
			JSONObject json = (JSONObject) jsonObj;

			for (String s : json.keySet()) 
				settable.set(s, json.get(s));
		}
		return (E) settable;
	}
	
	public class FiletreeMetaImpl implements FileTreeMeta {
		FileTree filetree;

		@Override
		public PathWrap getSource() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public PathWrap getTarget() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public FileTree getFileTree() {
			return filetree;
		}

	}

}
