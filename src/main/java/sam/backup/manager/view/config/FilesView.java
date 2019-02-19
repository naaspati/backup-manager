package sam.backup.manager.view.config;

import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sam.backup.manager.config.PathWrap;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.Status;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.config.Session;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxGridPane;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.io.serilizers.StringWriter2;
import sam.myutils.Checker;
import sam.nopkg.Junk;
import sam.reference.WeakPool;

public class FilesView extends BorderPane {
	private static WeakPool<FilesView> views = new WeakPool<>(() -> new FilesView(null, null, null, null)); 
	private static WeakReference<Stage> weakStage = new WeakReference<Stage>(null);
	private static final Session SESSION = Session.getSession(FilesView.class);

	private static Stage getStage() {
		Stage stage = weakStage.get();

		if(stage == null) {
			stage = new Stage();
			Scene scene = new Scene(new HBox());
			scene.getStylesheets().add("styles.css");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(Utils.window());
			stage.initStyle(StageStyle.UTILITY);
			stage.setScene(scene);
			stage.setWidth(600);

			weakStage = new WeakReference<Stage>(stage);
		}
		return stage;
	}
	public static void open(String string, IdentityHashMap<PathWrap, FileTree> fileTree2, FilesViewSelector all) {
		//TODO
		Junk.notYetImplemented();
	}
	public static Stage open(String title, FileTree filetree, Config config, Dir treeToDisplay, FilesViewSelector selector,  EventHandler<WindowEvent> onCloseRequest, Button...buttons) {
		Stage stage = getStage();
		FilesView[] vs = {null};
		
		views.forEach(f -> {
			if(vs[0] != null && f.treeToDisplay == treeToDisplay && f.selector == selector)
				vs[0] = f;
		});
		
		FilesView v = vs[0];
		
		if(v == null) {
			v = new FilesView(filetree, treeToDisplay, selector, buttons);
			views.add(v);
		}

		stage.getScene().setRoot(v);
		stage.setTitle(title);
		if(onCloseRequest != null)
			stage.setOnCloseRequest(onCloseRequest);
		stage.show();

		return stage;
	}
	private static final String separator = "    ";

	private final TreeView<FileEntity> treeView;
	private final ToggleButton expandAll = new ToggleButton("Expand All");
	private final SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
	private final SimpleIntegerProperty totalCount = new SimpleIntegerProperty();
	private final String sourceRoot, targetRoot;

	private final Dir treeToDisplay;
	private final AboutPane aboutPane = new AboutPane();
	private final FilesViewSelector selector;
	private final FileTree fileTree;

	private FilesView(FileTree filetree, Dir treeToDisplay, FilesViewSelector selector, Button[] buttons) {
		addClass(this, "files-view");
		this.fileTree = filetree; 
		sourceRoot = fileTree.getSourcePath();
		targetRoot = fileTree.getBackupPath();
		this.treeToDisplay = treeToDisplay;
		this.selector = selector;

		treeView = new TreeView<>();
		treeView.getSelectionModel()
		.selectedItemProperty()
		.addListener((p, o, n) -> aboutPane.reset(n == null ? null : n.getValue()));

		if(selector.isSelectable())
			treeView.setCellFactory(CheckBoxTreeCell.forTreeView());

		setClass(expandAll, "expand-toggle");
		expandAll.setOnAction(e -> {
			boolean b = expandAll.isSelected();
			expandAll.setText(b ? "collapse all" : "expand all");
			expand(b, treeView.getRoot().getChildren());
		});

		aboutPane.setMinWidth(300);
		setCenter(new SplitPane(treeView, aboutPane));
		setTop(top());
		setBottom(bottom(buttons));
		init();
	}
	private Node top() {
		GridPane grid = new GridPane();

		grid.setHgap(5);
		grid.setVgap(5);

		grid.addRow(0, new Text("%source% = "), link(sourceRoot));
		grid.addRow(1, new Text("%target% = "), link(targetRoot));

		Text count = new Text();
		count.setId("files-view-count");
		count.textProperty().bind(Bindings.concat("selected/total: ", selectedCount, "/", totalCount));

		grid.addRow(3, expandAll, count);
		grid.setPadding(new Insets(5));

		return grid;
	}
	private Node link(String p) {
		if(p == null)
			return new Text("--");
		Hyperlink link = new Hyperlink(p.toString());
		link.setOnAction(e -> FileOpenerNE.openFile(new File(p)));
		link.setWrapText(true);
		return link;
	}
	private void expand(boolean expand, Collection<TreeItem<FileEntity>> root) {
		for (TreeItem<FileEntity> item : root) {
			item.setExpanded(true);
			expand(expand, item.getChildren());
		}
	}
	private Node bottom(Button[] buttons) {
		CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveAction());
		save.disableProperty().bind(selectedCount.isEqualTo(0));

		if(buttons.length == 0) {
			setBottom(save);
			BorderPane.setAlignment(save, Pos.CENTER_RIGHT);
			BorderPane.setMargin(save, new Insets(5));
			return save;
		}
		else {
			HBox box = new HBox(5, buttons);
			box.getChildren().add(0, save);
			box.setAlignment(Pos.CENTER_RIGHT);
			box.setPadding(new Insets(5));
			return box;
		}
	}
	private void saveAction() {
		FileTreeString ft = new FileTreeString(treeToDisplay);
		String s = SESSION.getProperty("last.visited", System.getenv("USERPROFILE"));
		if(s == null)
			s = ".";

		//FIXME Utils.saveToFile2()

		File file = Utils.selectFile(new File(s), new File(treeToDisplay.getName()).getName()+".txt", "save File Tree").showSaveDialog(Utils.window());
		if(file == null) {
			FxPopupShop.showHidePopup("CANCELLED", 1500);
			return;
		}
		SESSION.put("last.visited", file.getParent());
		try {
			StringWriter2.setText(file.toPath(), ft.toString());
		} catch (IOException e) {
			FxAlert.showErrorDialog(file, "failed to save filetree", e);
		}
	}
	private void init() {
		TreeItem<FileEntity> root = item(treeToDisplay);
		root.setExpanded(true);
		int total = walk(root, treeToDisplay);

		selectedCount.set(total);
		totalCount.set(total);
		treeView.setRoot(root);
	}
	private class Unit extends CheckBoxTreeItem<FileEntity> {
		final FileEntity file;
		public Unit(FileEntity file) {
			super(file, null, selector.get(file));
			this.file = file;

			if(!file.isDirectory())
				selectedProperty().addListener((p, o, n) -> set(n));
		}
		public void set(Boolean n) {
			if(n == null) return;
			selector.set(file, n);
			selectedCount.set(selectedCount.get() + (n ? 1 : -1));
		}
	} 

	private int walk(TreeItem<FileEntity> parent, Dir dir) {
		int total = 0;
		for (FileEntity f : dir) {
			TreeItem<FileEntity> item =  item(f);
			parent.getChildren().add(item);
			if(f.isDirectory())
				total += walk(item, (Dir)f);
			else
				total++;
		}
		return total;
	}
	private TreeItem<FileEntity> item(FileEntity f) {
		return selector.isSelectable() ? new Unit(f) : new TreeItem<FileEntity>(f);
	}
	private class AboutPane extends VBox {
		final Text name = new Text();
		final Hyperlink sourceLink = new Hyperlink();
		final Hyperlink trgtLink = new Hyperlink();
		final TextArea about = new TextArea();
		final StringBuilder sb = new StringBuilder();
		final GridPane grid = FxGridPane.gridPane(5);

		AboutPane() {
			super(10);
			this.setId("about-pane");

			EventHandler<ActionEvent> handler = e -> {
				String p = (String) ((Hyperlink)e.getSource()).getUserData();
				FileOpenerNE.openFileLocationInExplorer(new File(p));
			};
			sourceLink.setOnAction(handler);
			sourceLink.setWrapText(true);

			trgtLink.setOnAction(handler);
			trgtLink.setWrapText(true);

			setClass(grid, "grid");

			grid.addRow(0, new Text("name: "), name);
			grid.addRow(1, new Text("source: "), sourceLink);
			grid.addRow(2, new Text("target: "), trgtLink);

			about.setEditable(false);
			about.setPrefColumnCount(12);
			about.setMaxWidth(Double.MAX_VALUE);
			about.setMaxHeight(Double.MAX_VALUE);
			VBox.setVgrow(about, Priority.ALWAYS);

			RadioMenuItem item = new RadioMenuItem("wrap text");
			about.wrapTextProperty().bind(item.selectedProperty());
			ContextMenu menu = new ContextMenu(item);

			about.setContextMenu(menu);

			getChildren().addAll(grid, about);
			grid.setVisible(false); 
			about.setVisible(false);
		}

		void reset(FileEntity file) {
			if(file == null) {
				grid.setVisible(false); 
				about.setVisible(false);
				return;
			}

			name.setText(file.getName());

			String s = file.getSourcePath();
			String b = file.getBackupPath();

			set(sourceLink, s, true);
			set(trgtLink, b, false);

			sb.setLength(0);

			try {
				append("About Source: \n", file.getSourceAttrs());
				append("\nAbout Backup: \n", file.getBackupAttrs());

				if(file.isDirectory())
					return;

				Status status = file.getStatus();

				if(status.isBackupable()) {
					sb
					.append("\n\n-----------------------------\nWILL BE ADDED TO BACKUP   (")
					.append("reason: ").append(status.getBackupReason()).append(" ) \n")
					.append("copied to backup: ").append(status.isCopied() ? "YES" : "NO").append('\n');
				}
				if(status.isBackupDeletable()) {
					sb
					.append("\n\n-----------------------------\nWILL BE DELETED\n")
					.append("reason:\n");
					appendDeleteReason(file, getMoveMap());
				}
			} finally {
				about.setText(sb.toString());
				grid.setVisible(true); 
				about.setVisible(true);
			}
		}

		private Map<String, List<FileEntity>> _moveMap;
		private Map<String, List<FileEntity>> getMoveMap() {
			if(_moveMap != null) return _moveMap;
			_moveMap = new HashMap<>();

			for (FileEntity f : fileTree) 
				_moveMap.computeIfAbsent(f.getName(), s -> new ArrayList<>()).add(f);

			if(_moveMap.values().stream().allMatch(l -> l.size() < 2))
				return Collections.emptyMap();

			_moveMap.values().removeIf(l -> l.size() < 2);
			return _moveMap;
		}
		private void append(String heading, Attrs ak) {
			sb.append(heading);
			append("old:\n", ak.old());
			append("new:\n", ak.current());
		}
		private void append(String heading, Attr a) {
			if(a != null && (a.size != 0 || a.lastModified != 0)) {
				sb.append(separator).append(heading)
				.append(separator).append(separator).append("size: ").append(a.size == 0 ? "0" : bytesToString(a.size)).append('\n')
				.append(separator).append(separator).append("last-modified: ").append(a.lastModified == 0 ? "--" : millsToTimeString(a.lastModified)).append('\n');
			}
		}

		private void appendDeleteReason(FileEntity file, Map<String, List<FileEntity>> entities) {
			List<FileEntity> list = entities.get(file.getName());

			if(Checker.isEmpty(list) || list.size() == 1)
				sb.append("UNKNOWN\n");
			else {
				sb.append("Possibly moved to: \n");
				for (FileEntity f : list) { 
					if(f != file)
						sb.append(separator).append(subpath(f.getBackupPath(), false)).append('\n');
				}
			}
		}
		private void set(Hyperlink h, String path, boolean isSource) {
			if(path == null) {
				h.setText("--");
				h.setDisable(true);
				return;
			}
			h.setText(subpath(path, isSource).toString());
			h.setDisable(false);
			h.setUserData(path);
		}
		private Object subpath(String p, boolean isSource) {
			if(p == null)
				return "--";

			String prefix = isSource ? "%source%\\" : "%target%\\";   
			String start = isSource ? sourceRoot : targetRoot;

			if(start == null || !p.startsWith(start))
				return p;

			return prefix + p.substring(start.length());
		}
	}
}