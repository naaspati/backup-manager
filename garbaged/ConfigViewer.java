package sam.backup.manager.view;

import javafx.scene.Node;
import sam.fx.helpers.FxText;

public class ConfigViewer extends ScrollPane2<BackupView> implements Viewer {
	private static volatile ConfigViewer instance;

	public static ConfigViewer getInstance() {
		if (instance == null) {
			synchronized (ConfigViewer.class) {
				if (instance == null)
					instance = new ConfigViewer();
			}
		}
		return instance;
	}
	
	private ConfigViewer() {
		super();
	}
	@Override
	public Node disabledView() {
		return FxText.text("No Backup Tasks Found", DISABLE_TEXT_CLASS);
	}
}