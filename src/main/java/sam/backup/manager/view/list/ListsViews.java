package sam.backup.manager.view.list;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import sam.backup.manager.SelectionListener;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.inject.Backups;
import sam.backup.manager.inject.Injector;
import sam.fx.helpers.FxCss;
import sam.fx.helpers.FxHBox;
import sam.reference.WeakAndLazy;

@Singleton
public class ListsViews extends BorderPane implements SelectionListener {
	private static final Logger LOGGER = LogManager.getLogger(ListsViews.class);

	private VBox root;
	private ScrollPane rootSp;
	private FileTreeManager factory;
	private ConfigManager cm;
	private Provider<Injector> injector;
	private WeakAndLazy<UpdateMultipleView> umv;
	

	@Inject
	public ListsViews(Provider<Injector> injector) {
		this.injector = injector;
	}

	private boolean init = false;

	@Override
	public void selected() {
		if(init)
			return;

		init = true;
		Injector injector = this.injector.get();
		@SuppressWarnings("unchecked")
		Collection<? extends Config> backups = injector.instance(Collection.class, Backups.class);

		Node banner = UtilsFx.headerBanner("Lists"+(backups.isEmpty() ? "" : " ("+backups.size()+")"));
		
		if(backups.isEmpty()) {
			setTop(banner);
			setCenter(UtilsFx.bigPlaceholder("Nothing Specified"));
		} else {
			this.factory = injector.instance(FileTreeManager.class);
			this.umv = new WeakAndLazy<>(UpdateMultipleView::new);
			this.cm = injector.instance(ConfigManager.class);

			CheckBox cb = new CheckBox("save without asking");
			cb.setOnAction(e -> ListConfigView.saveWithoutAsking = cb.isSelected());

			Button updateMultiple = new Button("Update Multiple");
			updateMultiple.setOnAction(e -> umv.get().show());

			HBox buttons = new HBox(10, cb, updateMultiple);
			buttons.setPadding(new Insets(2, 5, 2, 5));
			buttons.setBorder(FxCss.border(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new BorderWidths(1, 0, 1, 0)));
			buttons.setAlignment(Pos.CENTER_LEFT);

			root  = new VBox(2);
			rootSp = new ScrollPane(root);

			root.setFillWidth(true);
			rootSp.setFitToWidth(true);
			rootSp.setHbarPolicy(ScrollBarPolicy.NEVER);

			backups.forEach(c -> root.getChildren().add(new ListConfigView(c,cm.getBackupLastPerformed(ConfigType.LIST, c),factory, this.injector)));

			setTop(new BorderPane(banner, null, null, buttons, null));
			setCenter(root);
		}

		this.injector = null;
	}
	public void start(ListConfigView e) {
		Config c = e.getConfig();

		if(c.getWalkConfig().getDepth() <= 0) {
			UtilsFx.showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getWalkConfig().getDepth(), null);
			return;
		}
		try {
			FileTree f = factory.readFiletree(c, TreeType.LIST, true);
			c.setFileTree(f);
		} catch (Exception e1) {
			UtilsFx.showErrorDialog(null, "failed to read TreeFile: ", e1);
			LOGGER.error("failed to read TreeFile	", e1);
			return;
		}
		//FIXME UtilsFx.runAsync(new WalkTask(c, WalkMode.SOURCE, e, e));
	}
	//FIXME @Override
	public void onComplete(ListConfigView e) {
		cm.putBackupLastPerformed(ConfigType.LIST, e.getConfig(), System.currentTimeMillis());
	}

	private class UpdateMultipleView implements EventHandler<ActionEvent> {
		final ListView<TempWrap> list = new ListView<>();
		final Button ok = new Button("OK");
		final Button cancel = new Button("CANCEL");
		final HBox buttons = FxHBox.buttonBox(ok, cancel);
		Node bottom, center;

		public UpdateMultipleView() {

			root.getChildren()
			.forEach(node -> {
				ListConfigView item = (ListConfigView) node;
				if(!item.isDisable())
					list.getItems().add(new TempWrap(item));
			});

			list.setCellFactory(CheckBoxListCell.forListView(l -> l));
			
			ok.setOnAction(this);
			cancel.setOnAction(this);
		}

		void hide() {
			if(center == null)
				return;
			
			setBottom(bottom);
			setCenter(center);
			
			center = null;
			bottom = null;
		}
		void show() {
			center = getCenter();
			bottom = getBottom();
			
			setCenter(list);
			setBottom(buttons);
		}

		@Override
		public void handle(ActionEvent event) {
			if(event.getSource() == ok) {
				list.getItems().forEach(t -> {
					if(t.get()) {
						t.item.setOnWalkCompleted(ListConfigView::save);
						t.item.start();
					}
				});
			}
			hide();
		}
	}
	
	private class TempWrap extends SimpleBooleanProperty {
		final ListConfigView item;

		public TempWrap(ListConfigView item) {
			super(true);
			this.item = item;
		}
		@Override
		public String toString() {
			return item.getConfig().getSource().toString();
		}
	}
}
