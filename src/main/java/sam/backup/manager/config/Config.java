package sam.backup.manager.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config extends Base {
	private String source;
	private String target;
	private boolean disable;
	
	private transient Root root;
	private transient Path sourceP;
	private transient Path targetP;
	

	public Path getSource() {
		return sourceP = sourceP == null ? Paths.get(source) : sourceP;
	}
	public void setSource(String source) {
		setModified();
		this.source = source;
	}
	public Path getTarget() {
		if(targetP == null) {
			Path s = target == null ? getSource() : Paths.get(target);
			targetP = root.getFullBackupRoot().resolve(s.getRoot() == null ? s : s.subpath(0, s.getNameCount()));
		}
		
		return targetP;
	}
	public String getSourceText() {
		return source;
	}
	public void setRoot(Root root) {
		this.root = root;
	}
	public void disable() {
		disable = true;
	}
	public boolean isDisabled() {
		return disable;
	}
}
