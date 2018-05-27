package sam.backup.manager.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.Drive;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.extra.Options;

public class RootConfig extends ConfigBase {
	private static final long serialVersionUID = 1L;

	private String backupRoot;
	private Config[] backups;
	private Config[] lists;

	private transient Config[] _backups;
	private transient Config[] _lists;

	private static transient Path fullBackupRoot;
	
	public void init() {
		Path drive = Drive.DRIVE_LETTER;
		fullBackupRoot = drive == null ? null : backupRoot == null ? drive : drive.resolve(backupRoot);
	}
	public static Path fullBackupRoot() {
		return fullBackupRoot;
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
	@Override
	public Set<Options> getOptions() {
		if(_options != null) return _options;

		EnumSet<Options> temp = EnumSet.noneOf(Options.class);
		fill(this, temp);

		_options = Collections.unmodifiableSet(temp);

		return _options;
	}

	@Override
	public boolean isModified() {
		return super.isModified() || (getBackups() != null && Arrays.stream(getBackups()).anyMatch(Config::isModified));
	}
	public Path getFullBackupRoot() {
		return fullBackupRoot;
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
			Logger l = LogManager.getLogger(Config.class);
			StringBuilder sb = new StringBuilder();
			sb.append("-------- conflicting config.name --------\n");
			map.forEach((name, cnfs) -> {
				sb.append(name).append('\n');
				cnfs.forEach(c -> sb.append("    ").append(c.getSource()).append('\n'));
			});
			l.error(sb);
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
}
