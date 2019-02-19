package sam.backup.manager.config.api;

import java.util.List;
import java.util.Map;

import sam.backup.manager.config.PathWrap;
import sam.backup.manager.config.WalkConfig;
import sam.backup.manager.file.api.FileTree;

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
	public boolean isDisabled();
	public void setFileTree(FileTree f);
	public IFilter getSourceFilter();
	public IFilter getTargetFilter();
	public Map<PathWrap, FileTree> getFileTree();
	public IFilter getZipFilter();
}


