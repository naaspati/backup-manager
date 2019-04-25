import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.codejargon.feather.Feather;

import com.sun.javafx.application.LauncherImpl;

import sam.backup.manager.App;
import sam.backup.manager.PreloaderImpl;
import sam.config.LoadConfig;

@SuppressWarnings("restriction")
public class Main {
	public static void main(String[] args) throws URISyntaxException, IOException, SQLException {
		LoadConfig.load();
		LauncherImpl.launchApplication(App.class, PreloaderImpl.class, args);
	}
}
