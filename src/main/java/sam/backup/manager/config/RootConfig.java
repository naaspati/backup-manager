package sam.backup.manager.config;

import static sam.backup.manager.extra.VariablesKeys.DETECTED_DRIVE;
import static sam.backup.manager.extra.VariablesKeys.DETECTED_DRIVE_ID;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.extra.Utils;
import sam.myutils.MyUtilsCheck;
import sam.myutils.MyUtilsExtra;
import sam.myutils.System2;
import sam.reference.WeakAndLazy;
import sam.string.StringUtils;

public class RootConfig extends ConfigBase {
	private static final long serialVersionUID = 1L;

	private Config[] backups;
	private Config[]  lists;
	private Map<String, String> variables;

	private transient Config[] _backups;
	private transient Config[] _lists;

	RootConfig() {}
	
	@Override
	protected void init() {
		Map<String, List<Config>> string =  Stream.concat(backups == null ? Stream.empty() : Arrays.stream(backups), lists  == null ? Stream.empty() : Arrays.stream(lists))
				.peek(c -> {
					if(MyUtilsCheck.isEmptyTrimmed(c.getName()))
						throw new NullPointerException("name not specified: "+c);
				})
				.collect(Collectors.groupingBy(Config::getName));
		
		string.values().removeIf(l -> l.size() < 2);
		
		if(!string.isEmpty()) 
			new IllegalStateException("config.name conflict: "+string);
		
	}

	public Config findConfig(String name) {
		return find(name, getBackups());
	}
	public Config findList(String name) {
		return find(name, getLists());
	}
	private Config find(String name, Config[] cnf) {
		Objects.requireNonNull(name, "name cannot be null");
		return Arrays.stream(cnf).filter(c -> name.equals(c.getName())).findFirst().orElseThrow(() -> new NoSuchElementException("no config found for name: "+name)); 
	}
	public boolean hasLists() {
		return getLists() != null && getLists().length != 0;
	}
	public boolean hasBackups() {
		return  getBackups() != null && getBackups().length != 0;
	}
	public Config[] getLists() {
		if(_lists != null)
			return _lists;

		return _lists = filterAndPrepare(lists); 
	}
	private Config[] filterAndPrepare(Config[] configs) {
		if(configs == null || configs.length == 0)
			return null;

		Config[] cnf = Arrays.stream(configs)
				.filter(c -> c != null && !c.isDisabled())
				.peek(c -> c.init(this))
				.toArray(Config[]::new);

		Map<String, List<Config>> map = Arrays.stream(cnf).filter(c -> c.getName() != null).collect(Collectors.groupingBy(Config::getName));

		map.values().removeIf(l -> l.size() < 2);
		if(!map.isEmpty()) {
			Logger l = Utils.getLogger(Config.class);
			StringBuilder sb = new StringBuilder();
			sb.append("-------- conflicting config.name --------\n");
			map.forEach((name, cnfs) -> {
				sb.append(name).append('\n');
				cnfs.forEach(c -> sb.append("    ").append(c.getSource()).append('\n'));
			});
			l.error(sb.toString());
			System.exit(0);
		}
		return cnf;
	}
	public Config[] getBackups() {
		if(_backups != null)
			return _backups;

		return _backups = filterAndPrepare(backups);
	}
	@Override
	public IFilter getSourceFilter() {
		return excludes;
	}
	@Override
	public IFilter getTargetFilter() {
		return targetExcludes;
	}
	@Override
	protected RootConfig getRoot() {
		return this;
	}
	private WeakAndLazy<Pattern> pattern = new WeakAndLazy<>(() -> Pattern.compile("%(.+?)%"));

	public String resolve(String variable) {
		if(!StringUtils.contains(variable, '%'))
			return variable;

		StringBuffer sb = new StringBuffer();
		sb.setLength(0);
		Matcher m = this.pattern.get().matcher(variable);

		while(m.find()) {
			String s = getVariable(m.group(1));
			if(s == null) return null;
			m.appendReplacement(sb, Matcher.quoteReplacement(s));
		}
		m.appendTail(sb);
		
		return sb.toString();
	}

	public String getVariable(final String variable) {
		String result = System2.lookup(variable);
		result = result != null ? result : variables.get(variable);

		if(result == null) {
			if(variable.equals(DETECTED_DRIVE) || variable.equals(DETECTED_DRIVE_ID)) {
				DetectDrive detectDrive = new DetectDrive();
				variables.put(DETECTED_DRIVE, MyUtilsExtra.ifNotNull(detectDrive.getDrive(), Path::toString));
				variables.put(DETECTED_DRIVE_ID, detectDrive.getId());
			}
			
			result = variables.get(variable);

			if(result == null) {
				logger().error("value not found for variable: "+variable);
				return null;
			}
		}
		if(StringUtils.contains(result, '%')) {
			result = resolve(result);
			variables.put(variable, result);
		}
		return result;
	}
}

