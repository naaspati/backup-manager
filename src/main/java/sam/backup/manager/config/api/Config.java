package sam.backup.manager.config.api;

import java.util.List;

public interface Config {
	public String getName() ;
	public List<FileTreeMeta> getFileTreeMetas();
	public BackupConfig getBackupConfig() ;
	public WalkConfig getWalkConfig() ;
	public boolean isDisabled();
	public Filter getSourceExcluder();
	public Filter getTargetExcluder();
	public Filter getZipSelector();
	public ConfigType getType();
}


