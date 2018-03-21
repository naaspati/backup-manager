package sam.backup.manager.viewers;

import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.removeClass;
import static sam.fx.helpers.FxHelpers.setClass;

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
import sam.backup.manager.view.TransferView;
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
		setCenter(b, backupBtn, ConfigViewer.getInstance());
		setCenter(b, transferBtn, TransferViewer.getInstance());
		setCenter(b, listingBtn,ListViewer.getInstance());
	}

	private void setCenter(Button source, Button expected, Node node) {
		if(source == expected) { 
			setCenter(node.isDisabled() ? ((Viewer)node).disabledView() : node);
			activeBtn = source;
		}
	}
	public void add(Node c) {
		if(c == null)
			return;

		if(c instanceof ConfigView) 
			ConfigViewer.getInstance().add((ConfigView) c);
		else if(c instanceof  ListingView) 
			ListViewer.getInstance().add((ListingView) c);
		else 
			TransferViewer.getInstance().add((TransferView) c);
	}

	public void addAllListView(List<ListingView> list) {
		ListViewer.getInstance().addAll(list);
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
				ConfigViewer.getInstance().setDisable(disable);
				break;
			case TRANSFER:
				TransferViewer.getInstance().setDisable(disable);
				break;
			case LIST:
				ListViewer.getInstance().setDisable(disable);
				break;
		}
		if(activeBtn != null)
			activeBtn.fire();
	}
}
