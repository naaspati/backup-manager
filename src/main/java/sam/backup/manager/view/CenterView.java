package sam.backup.manager.view;

import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.removeClass;
import static sam.fx.helpers.FxHelpers.setClass;
import static sam.fx.helpers.FxHelpers.text;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.config.view.ListingView;

public class CenterView extends BorderPane implements EventHandler<ActionEvent> {
	private Button backupBtn;
	private Button transferBtn;
	private Button listingBtn;
	private HBox buttonBox = new HBox();

	private ScrollPane backupView, transferView, listView;
	private Node disableTransferView, disableListingView, disableBackupView;

	public CenterView() {
		addClass(this, "center-view");
		addClass(buttonBox, "top");
		setTop(buttonBox);
	}

	private void initView(ViewType type) {
		Button b = null;
		switch (type) {
			case TRANSFER:
				if(transferView != null) return;
				transferView = scrollpane();
				b = transferBtn = button("Transfer");
				break;
			case BACKUP:
				if(backupView  != null) return;
				backupView = scrollpane();
				b = backupBtn = button("Backups");
				break;
			case LIST:
				if(listView != null) return;
				listView = scrollpane();
				b = listingBtn = button("Listings");
				break;
		}
		buttonBox.getChildren().add(b);
	}
	public void add(Node c) {
		if(c == null)
			return;
		
		ScrollPane sp;
		if(c instanceof ConfigView) {
			initView(ViewType.BACKUP);
			sp = backupView;
		}else if(c instanceof  ListingView) {
			initView(ViewType.LIST);
			sp = listView;
		} else {
			initView(ViewType.TRANSFER);
			sp = transferView;
		} 
		addNode(sp, c);
	}
	public void addAllListView(List<ListingView> list) {
		initView(ViewType.LIST);
		((VBox)listView.getContent()).getChildren().addAll(list);
	}
	private void addNode(ScrollPane sp, Node c) {
		((VBox)sp.getContent()).getChildren().add(c);
	}

	@Override
	public void handle(ActionEvent e) {
		Button b = (Button)e.getSource(); 
		if(b.getStyleClass().contains("active"))
			return;

		if(backupBtn != null) removeClass(backupBtn, "active");
		if(transferBtn != null) removeClass(transferBtn, "active");
		if(listingBtn != null) removeClass(listingBtn, "active");

		addClass(b, "active");
		setCenter(b, backupBtn, disableBackupView, backupView);
		setCenter(b, transferBtn,disableTransferView, transferView);
		setCenter(b, listingBtn,disableListingView,listView);
	}
	private void setCenter(Button source, Button expected, Node disabled, Node original) {
		if(source == expected) 
			setCenter(disabled != null ? disabled : original);
	}

	private ScrollPane scrollpane() {
		VBox box = new VBox(2);
		box.setFillWidth(true);
		ScrollPane sp = new ScrollPane(box);
		sp.setFitToWidth(true);
		sp.setHbarPolicy(ScrollBarPolicy.NEVER);
		return sp;
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

	public void disable(ViewType type) {
		initView(type);

		switch (type) {
			case BACKUP:
				disableBackupView = text("No Backup Tasks Found", "disable-txt");
				break;
			case TRANSFER:
				disableTransferView = text("Nothing To Transfer", "disable-txt");
				break;
			case LIST:
				disableListingView = text("No Listing tasks Found", "disable-txt");
				break;
		}
	}
	public void enable(ViewType type) {
		switch (type) {
			case BACKUP:
				disableBackupView = null;
				break;
			case TRANSFER:
				disableTransferView = null;
				break;
			case LIST:
				disableListingView = null;
				break;
		}
	}
}
