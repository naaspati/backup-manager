package sam.backup.manager.view;

import javafx.scene.Node;

interface Viewer {
	static final String DISABLE_TEXT_CLASS = "disable-txt";
	public Node disabledView();
}
