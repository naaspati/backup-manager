import java.io.File;

import org.slf4j.LoggerFactory;

import javafx.application.Application;
import sam.backup.manager.App;
import sam.myutils.fileutils.FilesUtils;

public class Main  {
	public static void main(String[] args) {
		if(args.length == 1 && args[0].equals("open")) {
			FilesUtils.openFileNoError(new File("."));
			System.exit(0);
		}
		if(args.length == 1 && args[0].equals("-v")) {
			System.out.println("1.008");
			System.exit(0);
		}
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			LoggerFactory.getLogger(Main.class)
			.error("error at thread: "+thread.getName(), exception);
		});

		if(args.length == 1 && args[0].equals("--filter-tester"))
			Application.launch(FilterTester.class, args);
		else
			Application.launch(App.class, args);
	}
}
