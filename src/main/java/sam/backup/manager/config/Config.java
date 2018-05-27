package sam.backup.manager.config;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import sam.backup.manager.Drive;
import sam.backup.manager.config.filter.Filter;
import sam.backup.manager.config.filter.IFilter;
import sam.backup.manager.file.FileTree;

public class Config extends ConfigBase implements Serializable {
	private static final long serialVersionUID = 1L;

	private String source;
	private String target;
	private boolean disable;
	private Integer depth; 
	private StoringSetting storingMethod;
	private String name;
	
	private transient RootConfig root;
	private transient Path sourceP;
	private transient Path targetP;
	private transient FileTree fileTree;

	public Config(RootConfig root, Path source, Path target) {
		this.root = root;
		this.sourceP = source;
		this.targetP = target;
		this.source = source.toString();
		this.target = target.toString();
	}
	public Path getSource() {
		return sourceP != null ? sourceP :  (sourceP = _getSource());
	}
	private Path _getSource() {
		if(source.startsWith("%backupRoot%"))
			return Paths.get((root.getFullBackupRoot() == null ? "G:/Sameer" : root.getFullBackupRoot().toString()) + source.substring("%backupRoot%".length())); 

		return Paths.get(source);
	}
	public String getName() {
		return name;
	}
	public String getTargetString() {
		return target;	
	}
	public Path getTarget() {
		if(targetP == null && Drive.exists()) {
			Path t = null;
			if(target != null && (t = Paths.get(target)).getRoot() != null)
				targetP = t;
			else {
				Path s = target != null  ?  Paths.get(target) : getSource();
				targetP = root.getFullBackupRoot().resolve(s.getRoot() == null ? s : s.subpath(0, s.getNameCount())).normalize().toAbsolutePath();	
			}
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
	public boolean is1DepthWalk() {
		return getDepth() == 1 && Stream.of(excludes, targetExcludes, options).allMatch(t -> t == null);
	}
	public int getDepth() {
		return depth == null ? Integer.MAX_VALUE : depth;
	}
	public boolean isDisabled() {
		return disable;
	}
	public StoringSetting getStoringMethod() {
		return storingMethod == null ? StoringSetting.DIRECT_COPY : storingMethod;
	}
}
