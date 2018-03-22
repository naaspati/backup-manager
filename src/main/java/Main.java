import java.io.File;

import org.apache.logging.log4j.LogManager;

import javafx.application.Application;
import sam.backup.manager.App;
import sam.myutils.fileutils.FilesUtils;

public class Main  {
	public static void main(String[] args) {
		LogManager.getLogger(Main.class)
		.debug("error {} {}", 1 + 2, 2 + 2, new NullPointerException(), new NullPointerException(), new NullPointerException());
		
		if(args.length == 1 && args[0].equals("open")) {
			FilesUtils.openFileNoError(new File("."));
			System.exit(0);
		}
		if(args.length == 1 && args[0].equals("-v")) {
			System.out.println("1.008");
			System.exit(0);
		}
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LogManager.getLogger(Main.class).fatal("thread: {}", thread.getName(), exception));

		if(args.length == 1 && args[0].equals("--filter-tester"))
			Application.launch(FilterTester.class, args);
		else
			Application.launch(App.class, args);
	}
}
