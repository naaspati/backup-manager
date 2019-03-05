package sam.backup.manager.config.impl;

import java.util.List;

import sam.backup.manager.config.api.BackupConfig;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.api.IFilter;
import sam.backup.manager.config.api.WalkConfig;
import sam.myutils.Checker;

public class ConfigImpl implements Config {
	protected final String name;
	protected final List<FileTreeMeta> ftms;
	protected final boolean disable;
	
	protected final IFilter zip;
	protected final IFilter excludes;
	protected final IFilter targetExcludes;
	protected final BackupConfig backupConfig;
	protected final WalkConfig walkConfig;
	protected final ConfigType type;

	public ConfigImpl(
			String name, 
			ConfigType type,
			List<FileTreeMeta> ftms, 
			boolean disable, 
			IFilter zip,
			IFilter excludes, 
			IFilter targetExcludes, 
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
	@Override public IFilter getZipSelector() { return zip; }
	@Override public IFilter getSourceExcluder() { return excludes; }
	@Override public IFilter getTargetExcluder() { return targetExcludes; }
	@Override public BackupConfig getBackupConfig() { return backupConfig; }
	@Override public WalkConfig getWalkConfig() { return walkConfig; }
	@Override public ConfigType getType() { return type; }
}
