package sam.backup.manager.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import sam.backup.manager.file.FileTree;

public class Config extends ConfigBase {
	private String source;
	private String target;

	private transient RootConfig root;
	private transient Path sourceP;
	private transient Path targetP;
	private transient FileTree fileTree;
	private transient boolean sourceWalkComplete;
	private transient List<FileTree> backupFilesList;

	public Config() {}

	public Config(RootConfig root, Path source, Path target) {
		this.root = root;
		this.sourceP = source;
		this.targetP = target;
		this.source = source.toString();
		this.target = target.toString();
	}

	public Path getSource() {
		return sourceP = sourceP == null ? Paths.get(source) : sourceP;
	}
	public Path getTargetPath() {
		if(targetP == null && !root.isNoDriveMode()) {
			Path s = target == null ? getSource() : Paths.get(target);
			targetP = root.getFullBackupRoot().resolve(s.getRoot() == null ? s : s.subpath(0, s.getNameCount()));
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
		return excluder = createExcluder(root.getSourceExcluder(), excludes);
	}
	@Override
	public Predicate<Path> getTargetExcluder() {
		if(targetExcluder != null) return targetExcluder;
		return targetExcluder = createExcluder(root.getTargetExcluder(), targetExcludes);
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
}
