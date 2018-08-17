package sam.backup.manager.viewers;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.extra.Utils;
import sam.fx.helpers.FxClassHelper;
import sam.fx.helpers.FxText;

public class ListViewer extends BorderPane implements Viewer {
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

	ScrollPane2<ListingView> sp = new ScrollPane2<ListingView>() {};

	@Override
	public Node disabledView() {
		return FxText.text("No Listing tasks Found", DISABLE_TEXT_CLASS);
	}
	private ListViewer() {
		FxClassHelper.setClass(this, "list-viewer");
		setCenter(sp);

		CheckBox cb = new CheckBox("save without asking");
		cb.setOnAction(e -> ListingView.saveWithoutAsking = cb.isSelected());

		Button updateMultiple = new Button("Update Multiple");
		updateMultiple.setOnAction(this::updateMultiple);

		HBox buttons = new HBox(10, cb, updateMultiple);
		buttons.setPadding(new Insets(5));
		buttons.setStyle("-fx-padding:5;-fx-border-color:lightgray;-fx-border-width: 1 0 1 0");
		buttons.setAlignment(Pos.CENTER_LEFT);
		setTop(buttons);
	}
	private void updateMultiple(Object ignore) {
		ListView<TempWrap> list = new ListView<>();
		sp.forEach(item -> {
			if(!item.isDisable())
				list.getItems().add(new TempWrap(item));
		});
		list.setCellFactory(CheckBoxListCell.forListView(l -> l));

		Button ok = new Button("OK");
		HBox box = new HBox(ok);
		box.setPadding(new Insets(5));
		box.setAlignment(Pos.CENTER_RIGHT);
		
		Stage stage = Utils.showStage(new BorderPane(list, null, null, box, null));
		ok.setOnAction(e -> {
			stage.hide();
			list.getItems().forEach(t -> {
				if(t.get()) {
					t.item.setOnWalkCompleted(ListingView::save);
					t.item.start();
				}
			});
		});
	}
	private class TempWrap extends SimpleBooleanProperty {
		final ListingView item;

		public TempWrap(ListingView item) {
			super(true);
			this.item = item;
		}
		@Override
		public String toString() {
			return item.getConfig().getSource().toString();
		}
	}
	public void addAll(List<ListingView> list) {
		sp.container.getChildren().addAll(list);
	}
	public void add(ListingView c) {
		sp.add(c);
	}
}
