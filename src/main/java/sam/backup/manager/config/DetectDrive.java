package sam.backup.manager.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DetectDrive {
	private final Path drive_letter;
	private final String id;

	public DetectDrive() {
		Logger log = LoggerFactory.getLogger(getClass());

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
		this.drive_letter = drive;
		this.id = id;

		log.info("DRIVE: "+drive_letter);
		log.info("ID: "+id);
	}
	
	public Path getDrive() {
		return drive_letter;
	}
	public String getId() {
		return id;
	}
}
