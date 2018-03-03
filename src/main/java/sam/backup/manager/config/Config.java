package sam.backup.manager.config;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import sam.backup.manager.file.FileTree;

public class Config extends ConfigBase implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String source;
	private String target;

	private transient RootConfig root;
	private transient Path sourceP;
	private transient Path targetP;
	private transient FileTree fileTree;
	private transient boolean sourceWalkComplete;
	private transient List<FileTree> backupFilesList;
	private transient List<FileTree> deleteBackupFilesList;

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
		if(source.startsWith("%root%") || source.startsWith("%ROOT%"))
			return Paths.get((root.getFullBackupRoot() == null ? "G:/Sameer" : root.getFullBackupRoot().toString()) + source.substring(6)); 
		
		return Paths.get(source);
	}
	public Path getTarget() {
		if(targetP == null && !RootConfig.isNoDriveMode()) {
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
	
	public String getSourceText() {
		return source;
	}
	public void setRoot(RootConfig root) {
		this.root = root;
	}
	@Override
	protected RootConfig getRoot() {
		return root;
	}
	@Override
	public Predicate<Path> getSourceExcluder() {
		if(excluder != null) return excluder;
		return excluder = createFilter(root.getSourceExcluder(), excludes);
	}
	@Override
	public Predicate<Path> getTargetExcluder() {
		if(targetExcluder != null) return targetExcluder;
		return targetExcluder = createFilter(root.getTargetExcluder(), targetExcludes);
	}
	@Override
	public Predicate<Path> getSourceIncluder() {
		if(includer != null) return includer;
		return includer = createFilter(includes);
	}
	public FileTree getFileTree() {
		return fileTree;
	}
	public void setFileTree(FileTree filetree) {
		this.fileTree = filetree;
	}
	public boolean isSourceWalkCompleted() {
		return sourceWalkComplete;
	}

	public void setSourceWalkCompleted(boolean sourceWalkComplete) {
		this.sourceWalkComplete = sourceWalkComplete;
	}
	public void setBackupFiles(List<FileTree> list) {
		backupFilesList = Collections.unmodifiableList(list);
	}
	public List<FileTree> getBackupFiles() {
		return backupFilesList;
	}
	public List<FileTree> getDeleteBackupFilesList() {
		return deleteBackupFilesList;
	}
	public void setDeleteBackupFilesList(List<FileTree> deleteBackupFilesList) {
		this.deleteBackupFilesList = Collections.unmodifiableList(deleteBackupFilesList);
	}
	public boolean is1DepthWalk() {
		return getDepth() == 1 && Stream.of(excludes, includes, targetExcludes, walkSkips).allMatch(t -> t == null || t.length == 0);
	}
}
