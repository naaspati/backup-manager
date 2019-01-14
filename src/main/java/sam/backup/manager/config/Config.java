package sam.backup.manager.config;

import java.util.List;

import sam.backup.manager.file.FileTree;

public interface Config {
	public String getName() ;
	public List<PathWrap> getSource() ;
	public PathWrap getTarget(PathWrap src) ;
	public PathWrap getBaseTarget();
	public boolean isDisable() ;
	public IFilter getZip() ;
	public IFilter getExcludes() ;
	public IFilter getTargetExcludes() ;
	public BackupConfig getBackupConfig() ;
	public WalkConfig getWalkConfig() ;
	public FileTree getFileTree();
	public boolean isDisabled();
	public void setFileTree(FileTree f);
	
}


