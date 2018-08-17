package sam.backup.manager.config;

import java.io.Serializable;
import java.nio.file.Path;

import sam.backup.manager.config.filter.Filter;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.file.FileTree;
import sam.myutils.MyUtilsExtra;

public class Config extends ConfigBase implements Serializable {
	private static final long serialVersionUID = 1L;

	private String source;
	private String target;
	private boolean disable;
	private StoringSetting storingMethod;
	private String name;
	
	private transient RootConfig root;
	private transient Path sourceP;
	private transient Path targetP;
	private transient FileTree fileTree;
	
	public Config() {}

	public Config(RootConfig root, Path source, Path target) {
		this.root = root;
		this.sourceP = source;
		this.targetP = target;
		this.source = source.toString();
		this.target = target.toString();
	}
	public Path getSource() {
		return sourceP != null ? sourceP :  (sourceP = pathResolve(root.resolve(source)));
	}
	public String getName() {
		return name;
	}
	public Path getTarget() {
		if(targetP == null) {
			targetP = target == null ? null : pathResolve(root.resolve(target));
			
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
		return targetP;
	}
	public void init(RootConfig root) {
		this.root = root;
			
		if(storingMethod != null) {
			IFilter f = this.storingMethod.getSelecter();
			if(f instanceof Filter)
				((Filter)f).setConfig(this);	
		}
		
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
		return disable;
	}
	public StoringSetting getStoringMethod() {
		return MyUtilsExtra.nullSafe(storingMethod, StoringSetting.DIRECT_COPY);
	}

	public String getSourceRaw() {
		return source;
	}
	public String getTargetRaw() {
		return target;
	}
}
