package sam.backup.manager.config.api;

public interface WalkConfig {
	public boolean walkBackup();
	public boolean skipDirNotModified() ;
	public boolean skipFiles() ;
	public int getDepth() ;
}
