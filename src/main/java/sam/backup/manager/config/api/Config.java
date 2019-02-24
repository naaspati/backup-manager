package sam.backup.manager.config.api;

import java.util.List;

public interface Config {
	public String getName() ;
	public List<FileTreeMeta> getFileTreeMetas();
	public BackupConfig getBackupConfig() ;
	public WalkConfig getWalkConfig() ;
	public boolean isDisabled();
	public IFilter getSourceExcluder();
	public IFilter getTargetExcluder();
	public IFilter getZipSelector();
}


