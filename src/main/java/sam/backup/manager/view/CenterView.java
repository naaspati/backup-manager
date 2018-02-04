package sam.backup.manager.view;

import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.removeClass;
import static sam.fx.helpers.FxHelpers.setClass;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CenterView extends BorderPane implements EventHandler<ActionEvent> {
	private Button backupBtn;
	private Button copyingBtn;
	private Button listingBtn;
	private HBox buttonBox = new HBox();

	private Accordion backupView;
	private VBox copyingView,listingView;

	private ScrollPane copyingViewSP, listingViewSP;

	public CenterView() {
		addClass(this, "center-view");
		addClass(buttonBox, "top");
		setTop(buttonBox);
	}

	private void initCopyingView() {
		if(copyingView != null) return;
		copyingView = new VBox(2);
		copyingViewSP = scrollpane(copyingView);
		copyingBtn = button("Copying");
		buttonBox.getChildren().add(copyingBtn);
	}
	private void initBackupView() {
		if(backupView  != null) return;
		backupView = new Accordion();
		backupBtn = button("Backups");
		buttonBox.getChildren().add(backupBtn);
	}
	private void initListingView() {
		if(listingView != null) return;
		listingView = new VBox(2);
		listingViewSP = scrollpane(listingView);
		listingBtn = button("Listings");
		buttonBox.getChildren().add(listingBtn);
	}
	public void add(Node c) {
		if(c instanceof ConfigView) {
			initBackupView();
			backupView.getPanes().add((ConfigView)c);
		}else if(c instanceof  ListingView) {
			initListingView();
			listingView.getChildren().add(c);
		} else {
			initCopyingView();
			copyingView.getChildren().add(c);
		} 
	}

	@Override
	public void handle(ActionEvent e) {
		Button b = (Button)e.getSource(); 
		if(b.getStyleClass().contains("active"))
			return;

		getChildren().remove(getCenter());
		if(backupBtn != null) removeClass(backupBtn, "active");
		if(copyingBtn != null) removeClass(copyingBtn, "active");
		if(listingBtn != null) removeClass(listingBtn, "active");

		addClass(b, "active");
		if(b == backupBtn) setCenter(backupView);
		if(b == copyingBtn) setCenter(copyingViewSP);
		if(b == listingBtn) setCenter(listingViewSP);
	}

	private ScrollPane scrollpane(Node v) {
		ScrollPane sp = new ScrollPane(v);
		// sp.setFitToHeight(true);
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
}
