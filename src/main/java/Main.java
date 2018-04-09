import java.io.File;

import org.apache.logging.log4j.LogManager;

import javafx.application.Application;
import sam.backup.manager.App;
import sam.fileutils.FilesUtils;

public class Main  {
	public static void main(String[] args) {
		if(args.length == 1 && args[0].equals("open")) {
			FilesUtils.openFileNoError(new File("."));
			System.exit(0);
		}
		if(args.length == 1 && args[0].equals("-v")) {
			System.out.println("1.02");
			System.exit(0);
		}
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LogManager.getLogger(Main.class).fatal("thread: {}", thread.getName(), exception));
		Application.launch(App.class, args);
	}
}
