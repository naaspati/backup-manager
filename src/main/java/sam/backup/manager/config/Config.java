package sam.backup.manager.config;

import java.util.List;

import sam.backup.manager.file.FileTree;

public interface Config {
	public String getName() ;
	public List<String> getSource() ;
	public String getTarget() ;
	public boolean isDisable() ;
	public IFilter getZip() ;
	public IFilter getExcludes() ;
	public IFilter getTargetExcludes() ;
	public BackupConfig getBackupConfig() ;
	public WalkConfig getWalkConfig() ;
	public FileTree getFileTree();
	public boolean isDisabled();
}
