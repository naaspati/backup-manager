package sam.backup.manager;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class Drive {
	
	public static final Path DRIVE_LETTER;
	public static final String ID;
	
	static {
		Logger log = LogManager.getLogger(Drive.class);
		
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
				id = p.getProperty("id", "0-default");
			} catch (IOException e) {
				log.error("failed to read: "+drive.resolve(".iambackup"), e);
			}
		}
		DRIVE_LETTER = drive;
		ID = id;
		
		log.info("DRIVE_LETTER: "+DRIVE_LETTER);
		log.info("ID: "+id);
	}
	public static boolean exists() {
		return DRIVE_LETTER != null;
	}
}
