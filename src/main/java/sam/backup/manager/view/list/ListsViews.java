package sam.backup.manager.view.list;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codejargon.feather.Feather;
import org.codejargon.feather.Key;

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
import javafx.stage.Stage;
import sam.backup.manager.SelectionListener;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Backups;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;
import sam.fx.helpers.FxHBox;
import sam.fx.helpers.FxUtils;
import sam.reference.WeakAndLazy;

@Singleton
public class ListsViews extends BorderPane implements SelectionListener {
	private static final Logger LOGGER = LogManager.getLogger(ListsViews.class);

	private VBox root;
	private ScrollPane rootSp;
	private FileTreeFactory factory;
	private Utils utils;
	private UtilsFx fx;
	private Provider<Feather> feather;
	private WeakAndLazy<UpdateMultipleView> umv;

	@Inject
	public ListsViews(Provider<Feather> feather) {
		this.feather = feather;
	}

	private boolean init = false;

	@Override
	public void selected() {
		if(init)
			return;

		init = true;
		Feather feather = this.feather.get();
		@SuppressWarnings("unchecked")
		Collection<? extends Config> backups = feather.instance(Key.of(Collection.class, Backups.class));
		this.fx = feather.instance(UtilsFx.class);

		Node banner = fx.headerBanner("Lists"+(backups.isEmpty() ? "" : " ("+backups.size()+")"));
		if(backups.isEmpty()) {
			setTop(banner);
			setCenter(fx.bigPlaceholder("Nothing Specified"));
			return;
		}

		this.utils = feather.instance(Utils.class);
		this.factory = feather.instance(FileTreeFactory.class);
		Helper helper = feather.instance(Helper.class);
		this.umv = new WeakAndLazy<>(UpdateMultipleView::new);

		CheckBox cb = new CheckBox("save without asking");
		cb.setOnAction(e -> ListConfigView.saveWithoutAsking = cb.isSelected());

		Button updateMultiple = new Button("Update Multiple");
		updateMultiple.setOnAction(e -> umv.get().show());

		HBox buttons = new HBox(10, cb, updateMultiple);
		buttons.setPadding(new Insets(2, 5, 2, 5));
		buttons.setBorder(FxUtils.border(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new BorderWidths(1, 0, 1, 0)));
		buttons.setAlignment(Pos.CENTER_LEFT);

		root  = new VBox(2);
		rootSp = new ScrollPane(root);

		root.setFillWidth(true);
		rootSp.setFitToWidth(true);
		rootSp.setHbarPolicy(ScrollBarPolicy.NEVER);

		backups.forEach(c -> root.getChildren().add(new ListConfigView(c,utils.getBackupLastPerformed("list:"+c.getSource()), helper)));

		setTop(new BorderPane(banner, null, null, buttons, null));
		setCenter(root);
		this.feather = null;
	}
	public void start(ListConfigView e) {
		Config c = e.getConfig();

		if(c.getWalkConfig().getDepth() <= 0) {
			fx.showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getWalkConfig().getDepth(), null);
			return;
		}
		try {
			FileTree f = factory.readFiletree(c, TreeType.LIST, true);
			c.setFileTree(f);
		} catch (Exception e1) {
			fx.showErrorDialog(null, "failed to read TreeFile: ", e1);
			LOGGER.error("failed to read TreeFile	", e1);
			return;
		}
		fx.runAsync(new WalkTask(c, WalkMode.SOURCE, e, e));
	}
	@Override
	public void onComplete(ListConfigView e) {
		utils.putBackupLastPerformed("list:"+e.getConfig().getSource(), System.currentTimeMillis());
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
