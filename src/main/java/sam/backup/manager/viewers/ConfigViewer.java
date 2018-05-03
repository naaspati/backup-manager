package sam.backup.manager.viewers;

import javafx.scene.Node;
import sam.backup.manager.config.view.ConfigView;
import sam.fx.helpers.FxText;

public class ConfigViewer extends ScrollPane2<ConfigView> implements Viewer {
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
		return FxText.of("No Backup Tasks Found", DISABLE_TEXT_CLASS);
	}
}
