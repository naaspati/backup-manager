package sam.backup.manager;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import sam.nopkg.EnsureSingleton;

public class FileStoreManager {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	private final List<FileStore> drives;
	private final Path backupDrive;
	private final String id;
	
	public FileStoreManager() {
		singleton.init();
		
		Logger log = Utils.getLogger(getClass());

		Path drive = null;
		for (Path p : FileSystems.getDefault().getRootDirectories()) {
			if(Files.exists(p.resolve(".iambackup"))) {
				drive = p;
				break;
			}
		}

		Properties p = new Properties();
		String id = null;

		if(drive != null) {
			try {
				p.load(Files.newInputStream(drive.resolve(".iambackup")));
				id = p.getProperty("id");
				if(id == null)
					id = Files.getFileStore(drive).getAttribute("volume:vsn").toString(); 
			} catch (IOException e) {
				log.error("failed to read: "+drive.resolve(".iambackup"), e);
			}
		}
		this.backupDrive = drive;
		this.id = id;

		log.info(() -> new JSONObject().put("DRIVE", backupDrive).put("id", this.id).toString());
		
		this.drives = StreamSupport.stream(FileSystems.getDefault().getFileStores().spliterator(), false).collect(Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), (ArrayList<FileStore> list) -> {
			list.trimToSize();
			return Collections.unmodifiableList(list);
		}));
	}
	
	public List<FileStore> getDrives() {
		return drives;
	}
	public Path getBackupDrive() {
		return backupDrive;
	}
	public String getId() {
		return id;
	}
}
