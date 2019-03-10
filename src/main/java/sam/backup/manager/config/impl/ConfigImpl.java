package sam.backup.manager.config.impl;

import java.util.List;

import sam.backup.manager.config.api.BackupConfig;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.api.Filter;
import sam.backup.manager.config.api.WalkConfig;
import sam.myutils.Checker;

public class ConfigImpl implements Config {
	protected final String name;
	protected final List<FileTreeMeta> ftms;
	protected final boolean disable;
	
	protected final Filter zip;
	protected final Filter excludes;
	protected final Filter targetExcludes;
	protected final BackupConfig backupConfig;
	protected final WalkConfig walkConfig;
	protected final ConfigType type;

	public ConfigImpl(
			String name, 
			ConfigType type,
			List<FileTreeMeta> ftms, 
			boolean disable, 
			Filter zip,
			Filter excludes, 
			Filter targetExcludes, 
			BackupConfig backupConfig, 
			WalkConfig walkConfig
			) {
		
		Checker.requireNonNull("name, ftms", name, ftms);
		
		this.type = type;
		this.name = name;
		this.ftms = ftms;
		this.disable = disable;
		this.zip = zip;
		this.excludes = excludes;
		this.targetExcludes = targetExcludes;
		this.backupConfig = backupConfig;
		this.walkConfig = walkConfig;
	}
	
	@Override public String getName() { return name; }
	@Override public List<FileTreeMeta> getFileTreeMetas() { return ftms; }
	@Override public boolean isDisabled() { return disable; }
	@Override public Filter getZipSelector() { return zip; }
	@Override public Filter getSourceExcluder() { return excludes; }
	@Override public Filter getTargetExcluder() { return targetExcludes; }
	@Override public BackupConfig getBackupConfig() { return backupConfig; }
	@Override public WalkConfig getWalkConfig() { return walkConfig; }
	@Override public ConfigType getType() { return type; }
}
