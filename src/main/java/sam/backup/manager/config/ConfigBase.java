package sam.backup.manager.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.filter.Filter;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.extra.Options;

abstract class ConfigBase implements Serializable {
	private static final long serialVersionUID = 1L;
	protected static final Logger LOGGER =  LogManager.getLogger(ConfigBase.class);
	
	protected Filter excludes;
	protected Filter targetExcludes;
	protected Options[] options;
	protected Boolean noBackupWalk;
	protected Boolean noModifiedCheck;

	protected transient IFilter excluder, targetExcluder, includer;
	private transient boolean modified = false; //  for when Configs can be modified 
	protected transient Set<Options> _options;

	protected abstract RootConfig getRoot();
	public abstract IFilter getTargetFilter();
	public abstract IFilter getSourceFilter();
	
	protected void init() {
		if(excludes != null)
			excludes.setConfig((Config) this);
		if(targetExcludes != null)
			targetExcludes.setConfig((Config) this);
	}

	public Set<Options> getOptions() {
		if(_options != null) return _options;

		EnumSet<Options> temp = EnumSet.noneOf(Options.class);
		fill(getRoot(), temp);
		fill(this, temp);
		
		_options = Collections.unmodifiableSet(temp);
		
		return _options;
	}
	protected void fill(ConfigBase config, EnumSet<Options> sink) {
		if(config.options == null || config.options.length == 0)
			return;
		if(config._options != null)
			sink.addAll(config._options);
		else
			for (Options w : options) sink.add(w);
	}
	public boolean isModified() {
		return modified;
	}
	protected void setModified() {
		this.modified = true;
	}
	public boolean isNoBackupWalk() {
		return either(noBackupWalk, getRoot().noBackupWalk, false);
	}
	public boolean isNoModifiedCheck() {
		return either(noModifiedCheck, getRoot().noModifiedCheck, false);
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
	protected <T> T either(T t1, T t2, T defaultValue) {
		if(t1 == null && t2 == null)
			return defaultValue;
		return t1 != null ? t1 : t2;
	}
}
