package sam.backup.manager;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;

@Singleton
public class FileStoreManager {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	private final List<FileStore> drives;
	
	@Inject
	public FileStoreManager(FileSystem fs) {
		singleton.init();
		
		FileStore[] drive = StreamSupport.stream(FileSystems.getDefault().getFileStores().spliterator(), false)
		.toArray(FileStore[]::new);
		
		this.drives = Collections.unmodifiableList(Arrays.asList(drive));
	}
	
	public List<FileStore> getDrives() {
		return drives;
	}

	public FileStore getBackupDrive() {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
}
