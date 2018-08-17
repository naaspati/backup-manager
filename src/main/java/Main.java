import java.io.File;
import java.nio.file.Paths;

import org.slf4j.LoggerFactory;

import javafx.application.Application;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.ConfigReader;
import sam.backup.manager.config.RootConfig;
import sam.fileutils.FileOpener;

public class Main  {
	public static void main(String[] args) {
		if(args.length == 1 && args[0].equals("-v")) {
			System.out.println("1.04");
			System.exit(0);
		}
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LoggerFactory.getLogger(Main.class).error("thread: {}", thread.getName(), exception));
		RootConfig config = new ConfigReader().read(Paths.get("D:\\importents_are_here\\eclipse_workplace\\javafx\\BackupManager\\compiled\\app_data\\configs\\seagate.json"));
		
		config.print();
		System.out.println();
		for(Config c:config.getBackups()) {
			System.out.println(c.getSourceRaw()+"\t"+c.getSource());
			System.out.println(c.getTargetRaw()+"\t"+c.getTarget());
		}
		// Application.launch(App.class, args);
	}
}
