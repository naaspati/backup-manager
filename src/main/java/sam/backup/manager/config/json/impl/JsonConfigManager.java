package sam.backup.manager.config.json.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static sam.string.StringUtils.contains;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import sam.backup.manager.AppConfig;
import sam.backup.manager.AppConfig.ConfigName;
import sam.backup.manager.FileStoreManager;
import sam.backup.manager.Injector;
import sam.backup.manager.Stoppable;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.BackupConfig;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.api.IFilter;
import sam.backup.manager.config.api.WalkConfig;
import sam.backup.manager.config.impl.ConfigImpl;
import sam.backup.manager.config.impl.FilterImpl;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.myutils.Checker;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.nopkg.SavedResource;
import sam.nopkg.TsvMapTemp;
import sam.reference.WeakPool;
import sam.string.StringResolver;

@Singleton
public class JsonConfigManager implements ConfigManager, Stoppable {
	public static final String  VARS = "vars";
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
	{
		singleton.init();
	}

	private final Logger logger = Utils.getLogger(JsonConfigManager.class);
	private Runnable backupLastPerformed_mod;
	private SavedResource<TsvMapTemp> backupLastPerformed;

	private JConfig root_config; 
	private List<ConfigImpl> backups, lists;
	private boolean driveFound;
	private Path listPath, backupPath;

	@Inject
	public JsonConfigManager(AppConfig config, Injector injector) throws IOException {
		listPath = config.appDataDir().resolve("saved-trees").resolve(ConfigType.LIST.toString());
		backupPath = listPath.resolveSibling(ConfigType.BACKUP.toString());

		Files.createDirectories(listPath);
		Files.createDirectory(backupPath);
		Path jsonPath = (Path)config.getConfig(ConfigName.CONFIG_PATH_JSON);

		driveFound = injector.instance(FileStoreManager.class).getBackupDrive() != null;

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

			Config temp = (Config) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Config.class}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return null;
				}
			});

			if(!json.has(NAME))
				json.put(NAME, "root-json-config");

			Map<String, String> globalvars = new HashMap<>();
			globalvars.put("DETECTED_DRIVE", Junk.notYetImplemented()); //FIXME

			root_config = config(temp, null, json, globalvars);

			globalvars.putAll(root_config.vars.map);

			backups = parseArray(root_config, ConfigType.BACKUP, json, globalvars);
			lists = parseArray(root_config, ConfigType.LIST, json, globalvars);
		}
	}

	private List<ConfigImpl> parseArray(ConfigImpl root,ConfigType type, JSONObject json, Map<String, String> globalvars) {
		String key = type == ConfigType.BACKUP ? BACKUPS : LISTS;

		Object obj = json.get(key);
		if(obj == null)
			return emptyList();
		if(!(obj instanceof JSONArray))
			throw new JSONException("expected type: JSONArray, found: "+obj.getClass()+", for key: "+key);

		JSONArray array = (JSONArray) obj;

		if(array.isEmpty())
			return emptyList();
		else if(array.length() == 1) 
			return singletonList(config(root, type, array.getJSONObject(0), globalvars));
		else {
			ConfigImpl config[] = new ConfigImpl[array.length()];

			for (int i = 0; i < config.length; i++) 
				config[i] = config(root, type, array.getJSONObject(i), globalvars);

			return unmodifiableList(Arrays.asList(config));	
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<Config> get(ConfigType type) {
		Objects.requireNonNull(type);
		return (List)(type == ConfigType.LIST ? lists : backups);
	}

	private static final String DEFAULT_STRING = new String();
	private static final Long ZERO = Long.valueOf(0);

	@Override
	public Long getBackupLastPerformed(ConfigType type, Config config) {
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
		backupLastPerformed.get().put(key(type, config), time == 0 ? DEFAULT_STRING : Long.toString(time));
		backupLastPerformed_mod.run();
	}

	@Override
	public void stop() throws Exception {
		backupLastPerformed.close();
	}
	public static List<String> getList(Object obj, boolean unmodifiable) {
		if(obj == null)
			return emptyList();
		else if(obj.getClass() == String.class)
			return singletonList((String)obj);
		else if(obj instanceof JSONArray) {
			JSONArray array = ((JSONArray) obj);
			if(array.isEmpty())
				return emptyList();
			if(array.length() == 1)
				return singletonList(array.getString(0));

			String[] str = new String[array.length()];
			for (int i = 0; i < array.length(); i++) 
				str[i] = array.getString(i);

			List<String> list = Arrays.asList(str);

			return unmodifiable ? unmodifiableList(list) : list;
		} else {
			throw new IllegalArgumentException("bad type: "+obj);	
		}
	}

	private class JFilter extends FilterImpl implements Settable {
		private JConfig config;

		@Override
		protected Path resolve(String path) {
			return Paths.get(config.vars.resolve(path, config));
		}

		@Override
		protected FileSystem fs() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void set(String key, Object value) {
			switch (key) {
				case "name":       this.name = array(value); break;
				case "glob":       this.name = array(value); break;
				case "regex":      this.name = array(value); break;
				case "path":       this.name = array(value); break;
				case "startsWith": this.name = array(value); break;
				case "endsWith":   this.name = array(value); break;
				case "classes":    this.name = array(value); break;
				case "invert":
					if(value != null) 
						this.invert = (FilterImpl) getFilter((JSONObject) value);
					break;
				default:
					throw new IllegalArgumentException("unknown key: "+key+", value: "+value);
			}
		}

		private String[] array(Object value) {
			if(value == null)
				return null;

			JSONArray array = (JSONArray) value;
			if(array.length() == 0)
				return null;

			String[] str = new String[array.length()];
			for (int i = 0; i < str.length; i++) 
				str[i] = array.getString(i);

			return str;
		}

		public void setConfig(JConfig config) {
			this.config = config;
			JsonConfigManager.setConfig(invert, config);
		}
	}

	public IFilter getFilter(JSONObject json, String key) {
		json = json.optJSONObject(key);
		return getFilter(json);
	}
	public IFilter getFilter(JSONObject json) {
		if(json == null)
			return (f -> false);

		JFilter filter = new JFilter();
		json.keySet().forEach(s -> filter.set(s, json.get(s)));
		return filter;
	}

	private final WeakPool<List<String>> listsPool = new WeakPool<>(ArrayList::new);
	private final WeakPool<StringBuilder> sbPool = new WeakPool<>(StringBuilder::new);

	private class Vars {
		private final Map<String, String> map;

		public Vars(JSONObject json, String key, Map<String, String> global) {
			this.map = getMap(json, key, global);
		}

		private Map<String, String> getMap(JSONObject json, String key, Map<String, String> global) {
			JSONObject obj = json.optJSONObject(key);
			if(obj == null || obj.isEmpty())
				return emptyMap();

			HashMap<String, String> map = new HashMap<>();
			json.keySet().forEach(s -> map.put(s, json.getString(s)));

			if(map.keySet().stream().anyMatch(s -> contains(s, '%')))
				throw new JSONException(map.keySet().stream().filter(s -> contains(s, '%')).reduce(new StringBuilder("invalid char in key: ["), (sb, s) -> sb.append('"').append(s).append(", "), StringBuilder::append).append("]").toString());

			if(map.values().stream().noneMatch(s -> contains(s, '%')))
				return map;


			List<String> buffer = listsPool.poll();
			buffer.clear();
			buffer.addAll(map.keySet());

			for (String k : buffer) 
				map.put(k, getByKey(k, map, global, 0));

			buffer.clear();
			listsPool.add(buffer);

			return map;
		}
		
		private String getByKey(String key, Map<String, String> source, Map<String, String> global, int count) { 
			String value = source.get(key);
			if(value == null)
				value = global.get(key);
			if(value == null)
				throw new IllegalArgumentException("no value found for var: "+key);
			
			return resolve(value, source, global, count);
		}
		
		private String resolve(String value, JConfig config) {
			return resolve(value, config.vars.map, root_config.vars.map, 0);
		}

		private String resolve(String value, Map<String, String> source, Map<String, String> global, int count) {
			if(!contains(value, '%')) 
				return value;

			if(count > source.size() + global.size())
				throw new StackOverflowError("possily a circular pointer, \nsource: "+source+"\nglobal: "+global);
			StringBuilder sb = sbPool.poll();
			sb.setLength(0);

			StringResolver.resolve(value, '%', sb, s -> getByKey(s, source, global, count + 1));

			value = sb.toString();

			sb.setLength(0);
			sbPool.add(sb);

			return value;
		}
	}

	private JConfig config(Config global, ConfigType type, JSONObject json, Map<String, String> globalVars) {
		try {

			Vars vars = new Vars(json, VARS, globalVars); //TODO

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
			if(Boolean.TRUE.equals(disabled)) {
				return new JConfig(vars, name, type, ftms, disabled, null, null, null, null, null);
			} else {
				IFilter zip = getFilter(json, ZIP_IF);
				IFilter excludes = getFilter(json, EXCLUDES);
				IFilter targetExcludes = getFilter(json, TARGET_EXCLUDES);
				BackupConfigImpl backupConfig = set(json.get(BACKUP_CONFIG), new BackupConfigImpl((BackupConfigImpl) global.getBackupConfig()));
				WalkConfigImpl walkConfig = set(json.get(WALK_CONFIG), new WalkConfigImpl((WalkConfigImpl) global.getWalkConfig()));

				JConfig config = new JConfig(vars, name, type, ftms, disabled, zip, excludes, targetExcludes, backupConfig, walkConfig);
				
				setConfig(zip,config);
				setConfig(excludes,config);
				setConfig(targetExcludes,config);
				
				return config;
			}
		} catch (Exception e) {
			throw new JSONException(e.getMessage()+"\n"+json, e);
		}
	}

	private static void setConfig(IFilter f, JConfig config) {
		if(f != null && f instanceof JFilter)
			((JFilter) f).setConfig(config);
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

	class JConfig extends ConfigImpl {
		private final Vars vars;

		public JConfig(Vars vars, String name, ConfigType type, List<FileTreeMeta> ftms, boolean disable, IFilter zip,
				IFilter excludes, IFilter targetExcludes, BackupConfig backupConfig, WalkConfig walkConfig) {
			super(name, type, ftms, disable, zip, excludes, targetExcludes, backupConfig, walkConfig);

			this.vars = vars;

		}

		class FiletreeMetaImpl implements FileTreeMeta {
			FileTree filetree;
			final PathWrap source, target;

			public FiletreeMetaImpl(Config config, PathWrap source, PathWrap target) {
				this.source = source;
				this.target = target;
			}

			@Override
			public PathWrap getSource() {
				return source;
			}
			@Override
			public PathWrap getTarget() {
				return target;
			}
			@Override
			public FileTree getFileTree() {
				return filetree;
			}
			@Override
			public long getLastModified() {
				return Junk.notYetImplemented(); //FIXME
			}
			@Override
			public FileTree loadFiletree(FileTreeManager manager, boolean createNewIfNotExists) throws Exception {
				return this.filetree = manager.read(JConfig.this, getType() == ConfigType.LIST ? listPath : backupPath, createNewIfNotExists);
			}
		}
	}
}
