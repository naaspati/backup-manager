package sam.backup.manager.config;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.LoggerFactory;

import sam.backup.manager.config.filter.Filter;
import sam.backup.manager.config.filter.IFilter;

abstract class ConfigBase implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected Filter excludes;
	protected Filter targetExcludes;
	protected BackupConfig backupConfig; 

	protected transient IFilter excluder, targetExcluder, includer;

	protected abstract RootConfig getRoot();
	public abstract IFilter getTargetFilter();
	public abstract IFilter getSourceFilter();
	
	public BackupConfig getBackupConfig() {
		if(this.backupConfig == null)
			backupConfig = new BackupConfig();
		
		backupConfig.setRootConfig(getRoot().backupConfig);
		return backupConfig;
	}
	protected void init() {
		if(excludes != null)
			excludes.setConfig((Config) this);
		if(targetExcludes != null)
			targetExcludes.setConfig((Config) this);
	}
	protected static IFilter combine(IFilter root, IFilter self) {
		if(root == null && self == null)
			return (p -> false);
		if(root == null)
			return self;
		if(self == null)
			return root;

		return self.or(root);
	}
	protected org.slf4j.Logger logger() {
		return LoggerFactory.getLogger(getClass());
	}
	protected Path pathResolve(String s) {
		return s == null ? null : Paths.get(s);
	}
}
