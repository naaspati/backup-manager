import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import sam.backup.manager.extra.Utils;
import sam.config.LoadConfig;

public class Main  {
	public static void main(String[] args) throws URISyntaxException, IOException, SQLException {
		LoadConfig.load();
		Utils.init();

		//Dao dao = Dao.getInstance();

		new Converter();

		// Application.launch(App.class, args);
	}
}
