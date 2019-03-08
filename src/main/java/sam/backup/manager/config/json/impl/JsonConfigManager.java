package sam.backup.manager.config.json.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static sam.string.StringUtils.contains;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import sam.backup.manager.AppConfig;
import sam.backup.manager.AppConfig.ConfigName;
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
import sam.backup.manager.config.impl.PathWrap;
import sam.myutils.Checker;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.SavedResource;
import sam.nopkg.TsvMapTemp;
import sam.reference.WeakPool;
import sam.string.StringResolver;

@Singleton
public class JsonConfigManager implements ConfigManager, Stoppable {
	public static final String  DETECTED_DRIVE = "DETECTED_DRIVE";
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
	private final WeakPool<StringBuilder> sbPool = new WeakPool<>(StringBuilder::new);

	private Runnable backupLastPerformed_mod;
	private SavedResource<TsvMapTemp> backupLastPerformed;

	private Map<String, String> global_vars;
	private JConfig root_config; 
	private List<ConfigImpl> backups, lists;
	private Path listPath, backupPath;
	private final AppConfig appConfig;
	private final Path backupDrive;

	@Inject
	public JsonConfigManager(AppConfig config) throws IOException {
		listPath = config.appDataDir().resolve("saved-trees").resolve(ConfigType.LIST.toString());
		backupPath = listPath.resolveSibling(ConfigType.BACKUP.toString());
		this.appConfig = config;
		this.backupDrive = config.backupDrive();

		Files.createDirectories(listPath);
		Files.createDirectory(backupPath);
		Path jsonPath = (Path)config.getConfig(ConfigName.CONFIG_PATH_JSON);

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

		if(backupDrive == null)
			global_vars = Collections.emptyMap();
		else
			global_vars = Collections.singletonMap(DETECTED_DRIVE, backupDrive.toString());

		try(BufferedReader reader = Files.newBufferedReader(jsonPath)) {
			JSONObject json = new JSONObject(new JSONTokener(reader));	

			if(!json.has(NAME))
				json.put(NAME, "root-json-config");

			root_config = config(null, null, json);
			root_config.vars.putAll(global_vars);
			global_vars = root_config.vars; 

			backups = parseArray(root_config, ConfigType.BACKUP, json);
			lists = parseArray(root_config, ConfigType.LIST, json);
		}
	}

	private List<ConfigImpl> parseArray(ConfigImpl root,ConfigType type, JSONObject json) {
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
	public IFilter getFilter(JSONObject json, String key) {
		json = json.optJSONObject(key);
		return getFilter(json);
	}

	public IFilter getFilter(JSONObject json) {
		if(json == null)
			return (f -> false);

		JFilter2 filter = new JFilter2();
		set(json, filter);
		filter.init();
		return filter;
	}

	private class JFilter2 extends JFilter {
		@Override
		protected IFilter getFilter(JSONObject json) {
			return JsonConfigManager.this.getFilter(json);
		}

		public void init() {
			for (String[] sar : new String[][]{name, glob, regex, path, startsWith, endsWith, classes}) {
				if(Checker.isNotEmpty(sar)) {
					for (int i = 0; i < sar.length; i++) 
						sar[i] = resolve(sar[i], config.vars);
				}
			}
		}
	}

	private String detectedDrive;

	private String getByKey(String key, Map<String, String> source, int count) {
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
			source.replace(key, result);

			logger.debug("var resolved: \"{}\"=\"{}\" -> \"{}\"", key, value, result);
		}
		return result;

	}

	private String resolve(String value, Map<String, String> vars) {
		if(value == null)
			return value;
		return resolve(value, vars, 0);
	}

	private String resolve(String value, Map<String, String> source, int count) {
		if(!contains(value, '%')) 
			return value;

		if(count > source.size() + global_vars.size())
			throw new StackOverflowError("possily a circular pointer, \nsource: "+source+"\nglobal: "+global_vars);
		StringBuilder sb = sbPool.poll();
		sb.setLength(0);

		StringResolver.resolve(value, '%', sb, s -> getByKey(s, source, count + 1));

		if(!Checker.isEqual(sb, value))
			value = sb.toString();

		sb.setLength(0);
		sbPool.add(sb);

		return value;
	}

	private Map<String, String> map(JSONObject json, String key) {
		Object obj = json.opt(key);
		if(obj == null)
			return Collections.emptyMap();

		json = cast(obj, JSONObject.class);

		if(json.isEmpty())
			return Collections.emptyMap();

		Map<String, String> map = new HashMap<>();
		for (String s : json.keySet()) 
			map.put(s, cast(json.get(s), String.class));

		return map;
	}

	private <E> E cast(Object obj, Class<E> expected) {
		if(!expected.isInstance(obj))
			throw new JSONException(MessageFormat.format("expected: {0}, was: {1}", expected, obj.getClass()));
		return expected.cast(obj);
	}

	private JConfig config(Config global, ConfigType type, JSONObject json) {
		try {

			Map<String, String> vars = map(json, VARS);
			Function<Object, PathWrap> wrap = s -> PathWrap.of(resolve((String)s, vars));

			Object source = json.get(SOURCE);
			Object target = json.get(TARGET);

			List<FileTreeMeta> ftms = null;

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
			throw new JSONException(e.getMessage()+"\n"+json.toString(4), e);
		}
	}

	private List<FileTreeMeta> generateFtms(JSONArray source, Map<String, String> vars, BiFunction<Integer, PathWrap, PathWrap> targetGetter) {
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

	static void setConfig(IFilter f, JConfig config) {
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
		private final Map<String, String> vars;

		public JConfig(Map<String, String> vars, String name, ConfigType type, List<FileTreeMeta> ftms, boolean disable, IFilter zip,
				IFilter excludes, IFilter targetExcludes, BackupConfig backupConfig, WalkConfig walkConfig) {
			super(name, type, ftms, disable, zip, excludes, targetExcludes, backupConfig, walkConfig);

			this.vars = vars;

		}
		public Map<String, String> vars() {
			return vars;
		}

	}
}
