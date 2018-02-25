package sam.backup.manager.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import sam.backup.manager.file.Filter;
import sam.backup.manager.walk.WalkSkip;
import sam.console.ansi.ANSI;

public abstract class ConfigBase {
	protected String[] excludes;
	protected String[] includes;
	protected String[] targetExcludes;
	protected String[] walkSkips;
	protected Boolean noBackupWalk;
	protected Boolean noModifiedCheck;
	protected Boolean disable;
	protected Boolean deleteBackupIfSourceNotExists;
	private Integer depth; 

	protected transient Predicate<Path> excluder, targetExcluder, includer;
	private transient boolean modified = false; //  for when Configs can be modified 
	protected transient Set<WalkSkip> _walkSkips;

	protected abstract RootConfig getRoot();
	public abstract Predicate<Path> getTargetExcluder();
	public abstract Predicate<Path> getSourceExcluder();
	public abstract Predicate<Path> getSourceIncluder();

	public Set<WalkSkip> getWalkSkips() {
		if(_walkSkips != null) return _walkSkips;

		EnumSet<WalkSkip> temp = EnumSet.noneOf(WalkSkip.class);
		fill(getRoot(), temp);
		fill(this, temp);
		
		_walkSkips = Collections.unmodifiableSet(temp);
		
		return _walkSkips;
	}
	protected void fill(ConfigBase config, EnumSet<WalkSkip> sink) {
		if(config.walkSkips == null || config.walkSkips.length == 0)
			return;
		if(config._walkSkips != null)
			sink.addAll(config._walkSkips);
		else {
			for (String s : walkSkips) {
				if(s == null || s.isEmpty())
					continue;
				try {
					sink.add(WalkSkip.valueOf(s));
				} catch (IllegalArgumentException e) {
					System.out.println(ANSI.red("bad WalkSkip constant: ")+s);
				}
			}
		}
	}
	public int getDepth() {
		return depth == null ? Integer.MAX_VALUE : depth;
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
	public boolean isNoModifiedCheck() {
		return compareBooleans(noModifiedCheck, getRoot().noModifiedCheck);
	}
	public boolean istDeleteBackupIfSourceNotExists() {
		return compareBooleans(deleteBackupIfSourceNotExists, getRoot().deleteBackupIfSourceNotExists);
	}
	public Boolean isDisabled() {
		return compareBooleans(disable, getRoot().disable);
	}
	public void setDisabled(boolean disable) {
		this.disable = disable;
	}
	protected static Predicate<Path> createFilter(Predicate<Path> rootExcluder, String[] configExcludes) {
		if(rootExcluder == null && configExcludes == null)
			return (p -> false);
		if(rootExcluder == null)
			return createFilter(configExcludes);
		if(configExcludes == null)
			return rootExcluder;

		Filter e = new Filter(configExcludes);
		return e.isAlwaysFalse() ? rootExcluder : e.or(rootExcluder);
	}
	protected static Predicate<Path> createFilter(String[] excludes) {
		Filter e = new Filter(excludes);
		return e.isAlwaysFalse() ? (p -> false) : e; // e.isNoExclude() then Eclude e, can be left for grabage collection
	}
	protected boolean compareBooleans(Boolean b1, Boolean b2) {
		if(b1 == null && b2 == null)
			return false;
		return b1 != null ? b1 : b2;
	}
}
