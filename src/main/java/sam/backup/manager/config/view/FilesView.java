package sam.backup.manager.config.view;

import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.backup.manager.extra.Utils.saveToFile2;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.SimpleFileTreeWalker;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.fileutils.FileOpener;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxGridPane;
import sam.weak.WeakStore;

public class FilesView extends BorderPane {
	private static WeakStore<FilesView> views = new WeakStore<>(); 
	private static WeakReference<Stage> weakStage = new WeakReference<Stage>(null);

	private static Stage getStage() {
		Stage stage = weakStage.get();

		if(stage == null) {
			stage = new Stage();
			Scene scene = new Scene(new HBox());
			scene.getStylesheets().add("styles.css");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(App.getStage());
			stage.initStyle(StageStyle.UTILITY);
			stage.setScene(scene);
			stage.setWidth(600);

			weakStage = new WeakReference<Stage>(stage);
		}
		return stage;
	}
	public static Stage open(String title, Config config, DirEntity root, FilesViewMode mode, Button...buttons) {
		return open(title, config, root, mode, null, buttons);
	}
	public static Stage open(String title, Config config, DirEntity root, FilesViewMode mode,  EventHandler<WindowEvent> onCloseRequest, Button...buttons) {
		Stage stage = getStage();
		FilesView v = views.stream().filter(f -> f.fileTree == root && f.mode == mode).findFirst().orElse(null);
		if(v == null) {
			v = new FilesView(config, root, mode, buttons);
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

	private final TreeView<FileTreeEntity> treeView;
	private final ToggleButton expandAll = new ToggleButton("Expand All");
	private final SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
	private final SimpleIntegerProperty totalCount = new SimpleIntegerProperty();
	private final Path sourceRoot, targetRoot;

	private final DirEntity fileTree;
	private final AboutPane aboutPane = new AboutPane();
	private final FilesViewMode mode;

	private FilesView(Config config, DirEntity root, FilesViewMode mode, Button[] buttons) {
		addClass(this, "files-view");
		sourceRoot = config.getSource();
		targetRoot = config.getTarget();
		this.fileTree = root;
		this.mode = mode;

		treeView = new TreeView<>();
		treeView.getSelectionModel()
		.selectedItemProperty()
		.addListener((p, o, n) -> aboutPane.reset(n == null ? null : n.getValue()));

		if(mode != FilesViewMode.ALL)
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
	private Node link(Path p) {
		if(p == null)
			return new Text("--");
		Hyperlink link = new Hyperlink(p.toString());
		link.setOnAction(e -> App.getHostService().showDocument(p.toUri().toString()));
		link.setWrapText(true);
		return link;
	}
	private void expand(boolean expand, Collection<TreeItem<FileTreeEntity>> root) {
		for (TreeItem<FileTreeEntity> item : root) {
			item.setExpanded(true);
			expand(expand, item.getChildren());
		}
	}
	private Node bottom(Button[] buttons) {
		CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveToFile2(new FileTreeString(fileTree), Paths.get("D:\\Downloads").resolve(sourceRoot.getFileName()+".txt")));
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
	private void init() {
		TreeItem<FileTreeEntity> root = item(fileTree);
		root.setExpanded(true);
		int total = walk(root, fileTree);

		selectedCount.set(total);
		totalCount.set(total);
		treeView.setRoot(root);
	}
	private class Unit extends CheckBoxTreeItem<FileTreeEntity> {
		final FileTreeEntity file;
		public Unit(FileTreeEntity file) {
			super(file, null, mode.get(file));
			this.file = file;

			if(!file.isDirectory())
				selectedProperty().addListener((p, o, n) -> set(n));
		}
		public void set(Boolean n) {
			if(n == null) return;
			mode.set(file, n);
			selectedCount.set(selectedCount.get() + (n ? 1 : -1));
		}
	} 

	private int walk(TreeItem<FileTreeEntity> parent, DirEntity dir) {
		int total = 0;
		for (FileTreeEntity f : dir) {
			TreeItem<FileTreeEntity> item =  item(f);
			parent.getChildren().add(item);
			if(f.isDirectory())
				total += walk(item, (DirEntity)f);
			else
				total++;
		}
		return total;
	}
	private TreeItem<FileTreeEntity> item(FileTreeEntity f) {
		return mode.isSelectable() ? new Unit(f) : new TreeItem<FileTreeEntity>(f);
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
				Path p = (Path) ((Hyperlink)e.getSource()).getUserData();
				try {
					FileOpener.getInstance().openFileLocationInExplorer(p.toFile());
				} catch (IOException e1) {
					FxAlert.showErrorDialog(p, "failed to open location", e);
				}
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

			RadioMenuItem item = new RadioMenuItem("wrap text");
			about.wrapTextProperty().bind(item.selectedProperty());
			ContextMenu menu = new ContextMenu(item);

			about.setContextMenu(menu);

			getChildren().addAll(grid, about);
			grid.setVisible(false); 
			about.setVisible(false);
		}

		void reset(FileTreeEntity file) {
			if(file == null) {
				grid.setVisible(false); 
				about.setVisible(false);
				return;
			}

			name.setText(file.getfileNameString());

			Path s = file.getSourcePath();
			Path b = file.getBackupPath();

			set(sourceLink, s, true);
			set(trgtLink, b, false);

			sb.setLength(0);

			try {
				append("About Source: \n", file.getSourceAttrs());
				append("\nAbout Backup: \n", file.getBackupAttrs());

				if(file.isDirectory())
					return;

				if(file.isBackupable()) {
					sb
					.append("\n\n-----------------------------\nWILL BE ADDED TO BACKUP   (")
					.append("reason: ").append(file.getBackupReason()).append(" ) \n")
					.append("copied to backup: ").append(file.isCopied() ? "YES" : "NO").append('\n');
				}
				if(file.isDeletable()) {
					sb
					.append("\n\n-----------------------------\nWILL BE DELETED\n")
					.append("reason:\n");
					appendDeleteReason(file);
				}
			} finally {
				about.setText(sb.toString());
				grid.setVisible(true); 
				about.setVisible(true);
			}
		}

		private void append(String heading, AttrsKeeper ak) {
			sb.append(heading);
			append("old:\n", ak.getOld());
			append("new:\n", ak.getCurrent());
		}
		private void append(String heading, Attrs a) {
			if(a != null && (a.getSize() != 0 || a.getModifiedTime() != 0)) {
				sb.append(separator).append(heading)
				.append(separator).append(separator).append("size: ").append(a.getSize() == 0 ? "0" : bytesToString(a.getSize())).append('\n')
				.append(separator).append(separator).append("last-modified: ").append(a.getModifiedTime() == 0 ? "--" : millsToTimeString(a.getModifiedTime())).append('\n');
			}
		}
		private void appendDeleteReason(FileTreeEntity file) {
			List<FileTreeEntity> list = new ArrayList<>();
			Path name = file.getFileName();

			fileTree.walk(new SimpleFileTreeWalker() {
				@Override
				public FileVisitResult file(FileEntity ft) {
					if(ft != file && name.equals(ft.getFileName()))
						list.add(ft);
					return FileVisitResult.CONTINUE;
				}
			});
			if(list.isEmpty())
				sb.append("UNKNOWN\n");
			else {
				sb.append("Possibly moved to: \n");
				for (FileTreeEntity f : list) 
					sb.append(separator).append(subpath(f.getBackupPath(), false)).append('\n');
			}
		}
		private void set(Hyperlink h, Path path, boolean isSource) {
			if(path == null) {
				h.setText("--");
				h.setDisable(true);
				return;
			}
			h.setText(subpath(path, isSource).toString());
			h.setDisable(false);
			h.setUserData(path);
		}
		private Object subpath(Path p, boolean isSource) {
			String prefix = isSource ? "%source%\\" : "%target%\\";   
			Path start = isSource ? sourceRoot : targetRoot;

			if(p == null)
				return "--";
			if(start == null || start.getNameCount() == p.getNameCount() ||  !p.startsWith(start))
				return p;

			return prefix + p.subpath(start.getNameCount(), p.getNameCount());
		}
	}

}