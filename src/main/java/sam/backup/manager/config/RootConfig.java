package sam.backup.manager.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import sam.backup.manager.walk.WalkSkip;

public class RootConfig extends ConfigBase {
	private static boolean noDriveMode;

	public static boolean isNoDriveMode() {
		return noDriveMode;
	}

	private String backupRoot;
	private Config[] backups;
	private Config[] lists;

	private boolean backupsDisable;
	private boolean listsDisable;

	private transient Config[] _backups;
	private transient Config[] _lists;

	private transient Path fullBackupRoot;

	public void init(Path drive) {
		fullBackupRoot = drive == null ? null : backupRoot == null ? drive : drive.resolve(backupRoot);
		noDriveMode = fullBackupRoot == null;
	}
	
	@Override
	public Set<WalkSkip> getWalkSkips() {
		if(_walkSkips != null) return _walkSkips;

		EnumSet<WalkSkip> temp = EnumSet.noneOf(WalkSkip.class);
		fill(this, temp);
		
		_walkSkips = Collections.unmodifiableSet(temp);
		
		return _walkSkips;
	}

	@Override
	public boolean isModified() {
		return super.isModified() || (getBackups() != null && Stream.of(getBackups()).anyMatch(Config::isModified));
	}
	public Path getFullBackupRoot() {
		return fullBackupRoot;
	}
	public boolean hasLists() {
		return !listsDisable && getLists() != null && getLists().length != 0;
	}
	public boolean hasBackups() {
		return  !backupsDisable && getBackups() != null && getBackups().length != 0;
	}
	public Config[] getLists() {
		if(_lists != null)
			return _lists;

		return _lists = filterConfig(lists); 
	}
	private Config[] filterConfig(Config[] configs) {
		if(configs == null || configs.length == 0)
			return null;

		return Stream.of(configs)
				.filter(Objects::nonNull)
				.peek(c -> c.setRoot(this))
				.filter(c -> !c.isDisabled())
				.toArray(Config[]::new);
	}
	public Config[] getBackups() {
		if(_backups != null)
			return _backups;

		return _backups = filterConfig(backups);
	}
	@Override
	public Predicate<Path> getSourceExcluder() {
		if(excluder != null) return excluder;
		return excluder = createFilter(excludes);
	}
	@Override
	public Predicate<Path> getTargetExcluder() {
		if(targetExcluder != null) return targetExcluder;
		return targetExcluder = createFilter(targetExcludes);
	}
	@Override
	public Predicate<Path> getSourceIncluder() {
		throw new IllegalAccessError("Root cannot have a Includer");
	}
	@Override
	protected RootConfig getRoot() {
		return this;
	}
}
