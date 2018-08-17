package sam.backup.manager.viewers;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import sam.backup.manager.transfer.TransferView;
import sam.backup.manager.view.IUpdatable;
import sam.backup.manager.view.StatusView;
import sam.fx.helpers.FxClassHelper;
import sam.fx.helpers.FxText;

public class TransferViewer extends BorderPane implements Viewer, IUpdatable {
	
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
		FxClassHelper.setClass(this, "transfer-viewer");
		setCenter(container);
	}
	public void add(TransferView view) {
		container.add(view);
	}
	@Override
	public Node disabledView() {
		return FxText.text("Nothing To Transfer", DISABLE_TEXT_CLASS);
	}
	public void setStatusView(StatusView statusView) {
		setBottom(statusView);
	}
	@Override
	public void update() {
		container.stream().forEach(t -> t.update());
	}
}
