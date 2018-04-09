package sam.backup.manager.viewers;

import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.removeClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.transfer.TransferView;
import sam.backup.manager.view.IUpdatable;
import sam.backup.manager.view.ViewType;

public class ViewSwitcher extends BorderPane implements EventHandler<ActionEvent> {
	private final Button backupBtn = button("Backups");
	private final Button transferBtn = button("Transfer");
	private final Button listingBtn = button("Listings");
	private final HBox buttonBox = new HBox(backupBtn, listingBtn, transferBtn);

	private Button activeBtn;

	public ViewSwitcher() {
		addClass(this, "center-viewer");
		addClass(buttonBox, "top");
		setTop(buttonBox);
	}
	private static final String ACTIVE_CLASS = "active";
	@Override
	public void handle(ActionEvent e) {
		Button b = (Button)e.getSource(); 
		if(b.getStyleClass().contains(ACTIVE_CLASS))
			return;

		removeClass(ACTIVE_CLASS, backupBtn, transferBtn, listingBtn);

		addClass(b, ACTIVE_CLASS);
		setCenter(b, backupBtn, config());
		setCenter(b, transferBtn, transfer());
		setCenter(b, listingBtn,list());
	}

	private void setCenter(Button source, Button expected, Node node) {
		if(source == expected) { 
			setCenter(node.isDisabled() ? ((Viewer)node).disabledView() : node);

			if(node instanceof IUpdatable)
				((IUpdatable)node).update();

			activeBtn = source;
		}
	}
	public void add(ConfigView c) {
		config().add((ConfigView) c);
	}
	public void add(ListingView c) {
		list().add(c);
	}
	public void add(TransferView c) {
		transfer().add(c);
	}
	public void addAllListView(List<ListingView> list) {
		list().addAll(list);
	}
	private Button button(String s) {
		Button b = new Button(s);
		b.setOnAction(this);
		b.setMaxWidth(Double.MAX_VALUE);
		setClass(b, "btn");
		HBox.setHgrow(b, Priority.ALWAYS);
		return b;
	}

	public void firstClick() {
		if(!buttonBox.getChildren().isEmpty())
			((Button)buttonBox.getChildren().get(0)).fire();
	}

	public void setStatus(ViewType type, boolean disable) {
		switch (type) {
			case BACKUP:
				config().setDisable(disable);
				break;
			case TRANSFER:
				transfer().setDisable(disable);
				break;
			case LIST:
				list().setDisable(disable);
				break;
		}
		if(activeBtn != null)
			activeBtn.fire();
	}
	
	private ListViewer list;
	private ListViewer list() {
		return list != null ? list : (list = ListViewer.getInstance());
	}
	private TransferViewer transfer;
	private TransferViewer transfer() {
		return transfer != null ? transfer : (transfer = TransferViewer.getInstance());
	}
	private ConfigViewer config;
	private ConfigViewer config() {
		return config != null ? config : (config = ConfigViewer.getInstance());
	}
}
