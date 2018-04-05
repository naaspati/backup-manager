package sam.backup.manager.viewers;

import java.util.List;

import javafx.scene.Node;
import sam.backup.manager.config.view.ListingView;
import sam.fx.helpers.FxClassHelper;
import sam.fx.helpers.FxHelpers;

public class ListViewer extends ScrollPane2<ListingView> implements Viewer {
	private static volatile ListViewer instance;

	public static ListViewer getInstance() {
		if (instance == null) {
			synchronized (ListViewer.class) {
				if (instance == null)
					instance = new ListViewer();
			}
		}
		return instance;
	}
	@Override
	public Node disabledView() {
		return FxHelpers.text("No Listing tasks Found", DISABLE_TEXT_CLASS);
	}
	
	private ListViewer() {
		FxClassHelper.setClass(this, "list-viewer");
	}
	public void addAll(List<ListingView> list) {
		container.getChildren().addAll(list);
	}
}
