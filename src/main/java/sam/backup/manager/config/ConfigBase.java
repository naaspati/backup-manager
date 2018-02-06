package sam.backup.manager.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sam.backup.manager.walk.SkipBackupOption;

public abstract class ConfigBase {
	protected String[] excludes;
	protected String[] targetExcludes;
	protected SkipBackupOption[] backupSkips;
	protected Boolean noBackupWalk;
	protected Boolean disable;

	protected transient Predicate<Path> excluder, targetExcluder;
	private transient boolean modified = false; //  for when Configs can be modified 
	private transient Set<SkipBackupOption> _backupSkips;

	protected abstract RootConfig getRoot();
	public abstract Predicate<Path> getTargetExcluder();
	public abstract Predicate<Path> getSourceExcluder();

	public Set<SkipBackupOption> getBackupSkips() {
		if(_backupSkips != null) return _backupSkips;
		return _backupSkips = Collections.unmodifiableSet(
				rootSelfStream()
				.map(c -> c.backupSkips)
				.filter(Objects::nonNull)
				.flatMap(Stream::of)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(SkipBackupOption.class)))
				);
	}
	public boolean isNoDriveMode() {
		return getRoot().isNoDriveMode();
	}
	private Stream<ConfigBase> rootSelfStream() {
		return Stream.of(this, getRoot()).filter(Objects::nonNull);
	}
	public boolean isModified() {
		return modified;
	}
	protected void setModified() {
		this.modified = true;
	}
	public boolean isNoBackupWalk() {
		return compareBooleans(noBackupWalk, getRoot().noBackupWalk);
	}
	public Boolean isDisabled() {
		return compareBooleans(disable, getRoot().disable);
	}
	public void setDisabled(boolean disable) {
		this.disable = disable;
	}
	protected Predicate<Path> createExcluder(Predicate<Path> rootExcluder, String[] configExcludes) {
		if(rootExcluder == null && configExcludes == null)
			return (p -> false);
		if(rootExcluder == null)
			return createExclude(configExcludes);
		if(configExcludes == null)
			return rootExcluder;

		Exclude e = new Exclude(configExcludes);
		return e.isNoExclude() ? rootExcluder : e.or(rootExcluder);
	}
	protected Predicate<Path> createExclude(String[] excludes) {
		Exclude e = new Exclude(excludes);
		return e.isNoExclude() ? (p -> false) : e; // e.isNoExclude() then Eclude e, can be left for grabage collection
	}
	protected boolean compareBooleans(Boolean b1, Boolean b2) {
		if(b1 == null && b2 == null)
			return false;
		return b1 != null ? b1 : b2;
	}
}
