package sam.backup.manager.viewers;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.TransferView;
import sam.fx.helpers.FxHelpers;

public class TransferViewer extends BorderPane implements Viewer {
	
	private static volatile TransferViewer instance;

	public static TransferViewer getInstance() {
		if (instance == null) {
			synchronized (TransferViewer.class) {
				if (instance == null)
					instance = new TransferViewer();
			}
		}
		return instance;
	}

	private final ScrollPane2<TransferView> container = new ScrollPane2<TransferView>() {};
	
	private TransferViewer() {
		FxHelpers.setClass(this, "transfer-viewer");
	}
	public void add(TransferView view) {
		container.add(view);
		setCenter(container);
	}
	@Override
	public Node disabledView() {
		return FxHelpers.text("Nothing To Transfer", DISABLE_TEXT_CLASS);
	}
	public void setStatusView(StatusView statusView) {
		setBottom(statusView);
	}
}
