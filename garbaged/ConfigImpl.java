package sam.backup.manager.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.file.FileTree;

class ConfigImpl  {
	private List<Path> source;
	private Path target;
	private Map<String, FileTree> fileTree;
	private ConfigJsonImpl config;
	
	public ConfigImpl(ConfigJsonImpl config) {
		this.config = config;
	}
	public List<Path> getSource() {
		if(source != null)
			return source;
		
		return source != null ? source :  (source = pathResolve(root.resolve(source)));
	}
	public String getName() {
		return config.name;
	}
	public Path getTarget() {
		if(target == null) {
			target = target == null ? null : pathResolve(root.resolve(target));

			/*
			 * if(targetP == null)
				return null;

			 * if(t.getRoot() != null)
				targetP = t;
			else {
				Path s = target != null ?  pathResolve(root.resolve(target)) : getSource();
				targetP = root.getBackupRoot().resolve(s.getRoot() == null ? s : s.subpath(0, s.getNameCount())).normalize().toAbsolutePath();	
			}
			 */
		}
		return target;
	}
	public void init(RootConfig root) {
		this.root = root;

		if(zip != null) 
			zip.setConfig(this);	

		super.init();
	}
	@Override
	protected RootConfig getRoot() {
		return root;
	}
	@Override
	public IFilter getSourceFilter() {
		if(excluder != null) return excluder;
		return excluder = combine(root.getSourceFilter(), excludes);
	}
	@Override
	public IFilter getTargetFilter() {
		if(targetExcluder != null) return targetExcluder;
		return targetExcluder = combine(root.getTargetFilter(), targetExcludes);
	}
	public FileTree getFileTree() {
		return fileTree;
	}
	public void setFileTree(FileTree filetree) {
		this.fileTree = filetree;
	}
	public boolean isDisabled() {
		return config.disable;
	}
	@Override
	public String toString() {
		return "Config [name=" + config.name + ", source=" + source + ", target=" + target + ", disable=" + config.disable + "]";
	}

}
