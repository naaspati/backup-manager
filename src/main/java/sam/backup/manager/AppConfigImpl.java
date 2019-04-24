package sam.backup.manager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import javax.inject.Inject;

import sam.backup.manager.api.AppConfig;
import sam.backup.manager.api.FileStoreManager;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;

class AppConfigImpl implements AppConfig {
    public final Path app_data = Paths.get("app_data");
    private final Properties properties = new Properties(); 
    public final Path temp_dir;
    public final FileStoreManager fsm;
    
    @Inject
    public AppConfigImpl(FileStoreManager fsm) throws IOException {
        String apf = System2.lookup(AppConfig.class.getName()+".file");
        
        if(apf == null)
            throw new IOException("no property found: \""+AppConfig.class.getName()+".file\"");
        
        properties.load(new FileInputStream(apf));
        
        String dt = MyUtilsPath.pathFormattedDateTime();
        String dir = Stream.of(MyUtilsPath.TEMP_DIR.toFile().list())
                .filter(s -> s.endsWith(dt))
                .findFirst()
                .orElse(null);

        if(dir != null) {
            temp_dir = MyUtilsPath.TEMP_DIR.resolve(dir);
        } else {
            int n = Utils.number(MyUtilsPath.TEMP_DIR);
            temp_dir = MyUtilsPath.TEMP_DIR.resolve((n+1)+" - "+MyUtilsPath.pathFormattedDateTime());
            Files.createDirectories(temp_dir);      
        }
        
        this.fsm = fsm;
    }

    @Override
    public Path appDataDir() {
        return app_data;
    }
    @Override
    public Path tempDir() {
        return temp_dir;
    }
    @Override
    public String getConfig(String name) {
        return properties.getProperty(name);
    }
    @Override
    public Path backupDrive() {
        return fsm.getBackupDrive();
    }
}
