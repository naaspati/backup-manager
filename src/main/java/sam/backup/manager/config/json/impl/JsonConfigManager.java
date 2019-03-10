package sam.backup.manager.config.json.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static sam.string.StringUtils.contains;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import sam.backup.manager.AppConfig;
import sam.backup.manager.Stoppable;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.BackupConfig;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.api.Filter;
import sam.backup.manager.config.api.WalkConfig;
import sam.backup.manager.config.impl.ConfigImpl;
import sam.backup.manager.config.impl.PathWrap;
import sam.myutils.Checker;
import sam.myutils.ThrowException;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.nopkg.SavedResource;
import sam.nopkg.TsvMapTemp;
import sam.reference.WeakPool;
import sam.string.StringResolver;

class JsonConfigManager implements ConfigManager, Stoppable {
	public static final String  DETECTED_DRIVE = "DETECTED_DRIVE";

	public static final String  BACKUPS = "backups";
	public static final String  LISTS = "lists";

	public static final String  VARS = "vars";
	public static final String  NAME = "name";
	public static final String  SOURCE = "source";
	public static final String  TARGET = "target";
	public static final String  BACKUP_CONFIG = "backupConfig";
	public static final String  WALK_CONFIG = "walkConfig";
	public static final String  EXCLUDES = "excludes";
	public static final String  TARGET_EXCLUDES  = "targetExcludes"; 
	public static final String  DISABLE = "disable";
	public static final String  ZIP_IF = "zip";

	private Set<String> JCONFIG_VALID_KEYS, JFILTER_VALID_KEYS;

	private static final EnsureSingleton singleton = new EnsureSingleton();
	private final Logger logger;
	private final WeakPool<StringBuilder> sbPool;

	private Runnable backupLastPerformed_mod;
	private SavedResource<TsvMapTemp> backupLastPerformed;

	private Vars global_vars = new Vars(new HashMap<>());
	private List<ConfigImpl> backups, lists;
	private Path listPath, backupPath;
	private final Path backupDrive;

	public JsonConfigManager(AppConfig config) throws IOException {
		singleton.init();
		logger = Utils.getLogger(getClass());

		logger.debug("INIT {}", getClass());

		String js = config.getConfig(getClass().getName()+".file");
		if(js == null)
			throw new IllegalStateException("property not found: \""+getClass().getName()+".file"+"\"");

		Path jsonPath = Paths.get(js);
		if(!Files.isRegularFile(jsonPath))
			throw new IOException("file not found: \""+js+"\"");

		listPath = config.appDataDir().resolve("saved-trees").resolve(ConfigType.LIST.toString());
		backupPath = listPath.resolveSibling(ConfigType.BACKUP.toString());
		this.backupDrive = config.backupDrive();

		Files.createDirectories(listPath);
		Files.createDirectories(backupPath);

		sbPool = new WeakPool<>(StringBuilder::new);

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

		if(backupDrive != null)
			global_vars.map.put(DETECTED_DRIVE, backupDrive.toString());

		try(BufferedReader reader = Files.newBufferedReader(jsonPath)) {
			JSONObject json = new JSONObject(new JSONTokener(reader));

			JCONFIG_VALID_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(VARS,NAME,SOURCE,TARGET,BACKUP_CONFIG,WALK_CONFIG,EXCLUDES,TARGET_EXCLUDES ,DISABLE,ZIP_IF)));
			JFILTER_VALID_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(JFilter.validKeys())));

			JSONArray backups = get(json, BACKUPS, JSONArray.class);
			JSONArray lists = get(json, LISTS, JSONArray.class);

			json.remove(BACKUPS);
			json.remove(LISTS);

			json.put(NAME, "root");
			json.put(SOURCE, "root");
			json.put(TARGET, "root");

			Config proxy = new Config() {
				@Override public boolean isDisabled() { return ThrowException.illegalAccessError(); }
				@Override public Filter getZipSelector()  { return ThrowException.illegalAccessError(); }
				@Override public WalkConfig getWalkConfig() { return new WalkConfigImpl(null); }
				@Override public ConfigType getType()  { return ThrowException.illegalAccessError(); }
				@Override public Filter getTargetExcluder()  { return ThrowException.illegalAccessError(); }
				@Override public Filter getSourceExcluder()  { return ThrowException.illegalAccessError(); }
				@Override public String getName()  { return ThrowException.illegalAccessError(); }
				@Override public List<FileTreeMeta> getFileTreeMetas()  { return ThrowException.illegalAccessError(); }
				@Override public BackupConfig getBackupConfig() { return new BackupConfigImpl(null); }
			};

			JConfig root_config = config(proxy, null, json);
			global_vars.map.putAll(root_config.vars().map);

			this.backups = parseArray(root_config, ConfigType.BACKUP, backups);
			this.lists = parseArray(root_config, ConfigType.LIST, lists);

			JCONFIG_VALID_KEYS = null;
			JFILTER_VALID_KEYS = null;
		}
	}

	private List<ConfigImpl> parseArray(ConfigImpl root,ConfigType type, JSONArray array) {
		if(array == null || array.isEmpty())
			return emptyList();
		else if(array.length() == 1) 
			return singletonList(config(root, type, array.getJSONObject(0)));
		else {
			ConfigImpl config[] = new ConfigImpl[array.length()];

			for (int i = 0; i < config.length; i++) 
				config[i] = config(root, type, array.getJSONObject(i));

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

	private final Vars EMPTY_VARS = new Vars(Collections.emptyMap());

	class Vars {
		private final Map<String, String> map;

		public Vars(Map<String, String> map) {
			this.map = map;
		}

		public String get(String key) {
			return map.get(key);
		}

		public String resolve(String value) {
			return JsonConfigManager.this.resolve(value, this);
		}

		public int size() {
			return map.size();
		}
		public boolean containsKey(String key) {
			return map.containsKey(key);
		}
		public void replace(String key, String result) {
			map.replace(key, result);
		}
	}

	public Filter getFilter(JSONObject json, String key, Vars vars) {
		json = json.optJSONObject(key);
		return getFilter(json, vars);
	}

	public Filter getFilter(JSONObject json, Vars vars) {
		if(json == null || json.isEmpty())
			return null;

		JFilter filter = new JFilter(vars, (js, vrs) -> getFilter(js, vrs));
		validateKeys(json, JFILTER_VALID_KEYS, JFilter.class);
		set(json, filter);
		filter.init();
		return filter;
	}

	private String detectedDrive;

	private String getByKey(String key, Vars source, int count) {
		if(DETECTED_DRIVE.equals(key) && !source.containsKey(key)) {
			if(detectedDrive == null)
				detectedDrive = backupDrive == null ? "%"+key+"%" : backupDrive.toString();
			return detectedDrive;
		}

		String value = source.get(key);
		boolean global = false;

		if(value == null) {
			global = true;
			value = global_vars.get(key);
		}

		if(value == null)
			throw new IllegalArgumentException("no value found for var: "+key);

		String result = resolve(value, source, count);

		if(value != result) {
			if(global)
				global_vars.replace(key, result);
			else
				source.replace(key, result);

			logger.debug("var resolved: \"{}\"=\"{}\" -> \"{}\"", key, value, result);
		}
		return result;

	}

	private String resolve(String value, Vars vars) {
		return resolve(value, vars, 0);
	}

	private String resolve(String value, Vars source, int count) {
		if(value == null || !contains(value, '%')) 
			return value;

		if(count > source.size() + global_vars.size())
			throw new StackOverflowError("possily a circular pointer, \nsource: "+source+"\nglobal: "+global_vars);
		StringBuilder sb = sbPool.poll();
		sb.setLength(0);

		StringResolver.resolve(value, '%', sb, s -> getByKey(s, source, count + 1));

		if(!Checker.isEqual(sb, value)) {
			for (int i = 0; i < sb.length(); i++) {
				if(sb.charAt(i) == '/')
					sb.setCharAt(i, '\\');
			}
			
			String s = sb.toString();
			logger.debug("var resolved: \"{}\" -> \"{}\"", value, s);
			value = s;
		} else if(contains(value, '/'))
			value = value.replace('/', '\\');

		sb.setLength(0);
		sbPool.add(sb);

		return value;
	}

	private Vars vars(JSONObject json, String key) {
		Object obj = json.opt(key);
		if(obj == null)
			return EMPTY_VARS;

		json = cast(obj, JSONObject.class);

		if(json.isEmpty())
			return EMPTY_VARS;

		Map<String, String> map = new HashMap<>();
		for (String s : json.keySet()) 
			map.put(s, cast(json.get(s), String.class));

		return new Vars(map);
	}

	private <E> E get(JSONObject json, String key, Class<E> expected) {
		return cast(json.opt(key), expected);
	}
	private <E> E cast(Object obj, Class<E> expected) {
		if(obj == null)
			return null;

		if(!expected.isInstance(obj))
			throw new JSONException(MessageFormat.format("expected: {0}, was: {1}", expected, obj.getClass()));
		return expected.cast(obj);
	}

	private JConfig config(Config global, ConfigType type, JSONObject json) {
		try {
			validateKeys(json, JCONFIG_VALID_KEYS, JConfig.class);

			Vars vars = vars(json, VARS);
			Function<Object, PathWrap> wrap = s -> PathWrap.of(resolve((String)s, vars));

			List<FileTreeMeta> ftms = null;

			Object source = json.get(SOURCE);
			Object target = json.get(TARGET);

			if(source == null || target == null)
				throw new NullPointerException("source or/and target cannot be null");
			else if(isString(source) && isString(target))
				ftms = Collections.singletonList(new FiletreeMetaImpl(wrap.apply(source), wrap.apply(target)));
			else if(source instanceof JSONArray && isString(target)) {
				JSONArray array = (JSONArray) source;
				if(array.isEmpty())
					throw new IllegalArgumentException("empty source array");

				PathWrap targetP = wrap.apply(target);
				ftms = generateFtms(array, vars, (i, p) -> targetP.resolve(p.path().getFileName()));
			} else if(source instanceof JSONArray && target instanceof JSONArray) {
				JSONArray src = (JSONArray) source;
				JSONArray trgt = (JSONArray) target;

				if(src.length() != trgt.length())
					throw new IllegalArgumentException("source.length("+src.length()+") != target.length("+trgt.length()+")");

				ftms = generateFtms(src, vars, (i, p) -> wrap.apply(trgt.get(i)));
			} else {
				throw new IllegalArgumentException(String.format("invalid type of source(%s) or/and target(%s) ", source.getClass(), target.getClass()));
			}

			String name = json.getString(NAME);

			Boolean disabled = json.optBoolean(DISABLE, false);
			if(backupDrive == null || Boolean.TRUE.equals(disabled)) {
				return new JConfig(vars, name, type, ftms, disabled, null, null, null, null, null);
			} else {
				Filter zip = getFilter(json, ZIP_IF, vars);
				Filter excludes = getFilter(json, EXCLUDES, vars);
				Filter targetExcludes = getFilter(json, TARGET_EXCLUDES, vars);
				BackupConfigImpl backupConfig = createSettable(json.opt(BACKUP_CONFIG), (BackupConfigImpl)global.getBackupConfig(), b -> new BackupConfigImpl(b));
				WalkConfigImpl walkConfig = createSettable(json.opt(WALK_CONFIG), (WalkConfigImpl)global.getWalkConfig(), b -> new WalkConfigImpl(b));

				JConfig config = new JConfig(vars, name, type, ftms, disabled, zip, excludes, targetExcludes, backupConfig, walkConfig);

				return config;
			}
		} catch (Exception e) {
			throw new JSONException(e.getMessage()+"\n"+json.toString(4), e);
		}
	}

	private void validateKeys(JSONObject json, Set<String> validKeys, @SuppressWarnings("rawtypes") Class cls) {
		if(json.isEmpty())
			return;

		if(json.keySet().stream().anyMatch(s -> !validKeys.contains(s))) {
			StringBuilder sb = sbPool.poll();
			sb.setLength(0);
			sb.append("invalid key found: \nfor: ").append(cls)
			.append("\ninvalid Keys: [");
			json.keySet().forEach(s -> {
				if(!validKeys.contains(s))
					sb.append('"').append(s).append("\", ");
			});
			sb.setLength(sb.length() - 2);
			sb.append(']');

			String s = sb.toString();
			sb.setLength(0);
			sbPool.add(sb);

			throw new JSONException(s);
		}
	}

	private List<FileTreeMeta> generateFtms(JSONArray source, Vars vars, BiFunction<Integer, PathWrap, PathWrap> targetGetter) {
		if(source.isEmpty())
			Collections.emptyList();

		FiletreeMetaImpl[] result = new FiletreeMetaImpl[source.length()];

		for (int i = 0; i < result.length; i++) {
			PathWrap p = new PathWrap(resolve(source.getString(i), vars));
			PathWrap t = targetGetter.apply(i, p);
			result[i] = new FiletreeMetaImpl(p, t);
		}

		if(result.length != Arrays.stream(result).map(f -> f.target.path()).distinct().count()) {
			StringBuilder sb = new StringBuilder("overlapping targets: \n");
			Arrays.stream(result).collect(Collectors.groupingBy(f -> f.target.path()))
			.forEach((p, list) -> {
				sb.append("target: "+p);
				list.forEach(f -> sb.append("  source: ").append(f.source.path()).append('\n'));
			});
			throw new IllegalArgumentException(sb.toString());
		}
		return Arrays.asList(result);
	}

	private boolean isString(Object o) {
		return o.getClass() == String.class;
	}
	public <E extends Settable> E createSettable(Object jsonObj, E global, Function<E, E> creater) {
		if(jsonObj == null)
			return global;

		JSONObject json = cast(jsonObj, JSONObject.class);
		if(json.isEmpty())
			return global;

		return set(json, creater.apply(global));
	}

	private <E extends Settable> E set(JSONObject json, E value) {
		for (String s : json.keySet()) 
			value.set(s, json.get(s));

		return value;
	}

	class JConfig extends ConfigImpl {
		private final Vars _vars;

		public JConfig(Vars vars, String name, ConfigType type, List<FileTreeMeta> ftms, boolean disable, Filter zip,
				Filter excludes, Filter targetExcludes, BackupConfig backupConfig, WalkConfig walkConfig) {
			super(name, type, ftms, disable, zip, excludes, targetExcludes, backupConfig, walkConfig);

			this._vars = vars;

		}
		public Vars vars() {
			return _vars;
		}

	}
}
