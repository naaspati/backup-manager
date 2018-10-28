import javafx.application.Application;
import sam.backup.manager.App;
import sam.backup.manager.extra.Utils;

// import sam.logging.LogFilter;
public class Main  {
	public static void main(String[] args) {
		if(args.length == 1 && args[0].equals("-v")) {
			System.out.println("1.04");
			System.exit(0);
		}
		
		Utils.init();
		Application.launch(App.class, args);
	}
}
