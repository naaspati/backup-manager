package sam.backup.manager.view;

import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.filechooser.FileView;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.stage.Window;
import sam.backup.manager.ParentWindow;
import sam.backup.manager.StopTasksQueue;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.PathWrap;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.Status;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.fx.helpers.FxGridPane;
import sam.fx.helpers.FxHBox;
import sam.fx.helpers.FxUtils;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.Checker;
import sam.nopkg.SavedAsStringResource;

public class FilesView extends BorderPane {
	private static final String separator = "    ";

	private final TreeView<FileEntity> treeView = new TreeView<>();;
	private final ToggleButton expandAll = new ToggleButton("Expand All");
	private final SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
	private final SimpleIntegerProperty totalCount = new SimpleIntegerProperty();
	private final AboutPane aboutPane = new AboutPane();
	private final UtilsFx fx;
	
	private PathWrap sourceRoot, targetRoot;
	private Dir treeToDisplay;
	private FilesViewSelector selector;
	private FileTree fileTree;
	private final Provider<Window> parent;
	private final Utils utils;
	private static SavedAsStringResource<String> lastVisited;
	

	@Inject
	public FilesView(UtilsFx fx, Utils utils, StopTasksQueue queue, @ParentWindow Provider<Window> parent) {
		this.fx = fx;
		this.parent = parent;
		this.utils = utils;
		
		if(lastVisited == null) {
			lastVisited = new SavedAsStringResource<>(utils.appDataDir().resolve(getClass().getName()+".last.visited"), s -> s);
			queue.add(() -> {
				try {
					lastVisited.close();
				} catch (IOException e1) {
					utils.getLogger(FileView.class).warn("failed to save: {}", lastVisited.getSavePath(), e1);
				}
			});
		}
		
		addClass(this, "files-view");

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
	}

	public void init(Config config, FileTree filetree, Dir treeToDisplay, FilesViewSelector selector, Button[] buttons) {
		this.fileTree = filetree; 
		this.sourceRoot = fileTree.getSourcePath();
		this.targetRoot = fileTree.getBackupPath();
		this.treeToDisplay = treeToDisplay;
		this.selector = selector;
		
		bottom(buttons);
		init();		
	}
	private void init() {
		TreeItem<FileEntity> root = item(treeToDisplay);
		root.setExpanded(true);
		int total = walk(root, treeToDisplay);

		selectedCount.set(total);
		totalCount.set(total);
		treeView.setRoot(root);
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
	private Node link(PathWrap p) {
		if(p == null)
			return new Text("--");
		
		Hyperlink link = new Hyperlink(p.string());
		link.setOnAction(e -> FileOpenerNE.openFile(p.path().toFile()));
		link.setWrapText(true);
		return link;
	}
	private void expand(boolean expand, Collection<TreeItem<FileEntity>> root) {
		for (TreeItem<FileEntity> item : root) {
			item.setExpanded(true);
			expand(expand, item.getChildren());
		}
	}
	
	private final CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveAction());
	private HBox button_box;
	{
		save.disableProperty().bind(selectedCount.isEqualTo(0));
		BorderPane.setAlignment(save, Pos.CENTER_RIGHT);
		BorderPane.setMargin(save, new Insets(5));
	}
	
	private void bottom(Button[] buttons) {
		clearBottom();
		
		if(Checker.isEmpty(buttons)) 
			setBottom(save);
		 else 
			setButtons0(buttons);
	}
	public void setButtons(Node... buttons) {
		clearBottom();
		setButtons(buttons);	
	}
	private void setButtons0(Node[] buttons) {
		if(button_box == null)
			button_box = FxHBox.buttonBox(buttons);
		else {
			FxUtils.edit(button_box.getChildren(), list -> {
				list.clear();
				list.add(save);
				list.addAll(buttons);	
			});	
		}
		setBottom(button_box);
	}
	
	private void clearBottom() {
		setBottom(null);
		if(button_box != null)
			button_box.getChildren().removeIf(e -> e == save);
	}

	private void saveAction() {
		FileTreeString ft = new FileTreeString(treeToDisplay);
		String s = Optional.ofNullable(lastVisited.get()).orElseGet(() -> Optional.ofNullable(System.getenv("USERPROFILE")).filter(Checker::isNotEmptyTrimmed).orElse("."));
		
		File parent = new File(s);
		if(!parent.exists())
			parent = new File(".");

		//FIXME Utils.saveToFile2()

		File file = fx.fileChooser(parent, new File(treeToDisplay.getName()).getName()+".txt", "save File Tree").showSaveDialog(this.parent.get());
		if(file == null) {
			FxPopupShop.showHidePopup("CANCELLED", 1500);
			return;
		}
		
		lastVisited.set(file.getParent());
		utils.setTextNoError(file.toPath(), ft, "failed to save filetree: ");
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

			PathWrap s = file.getSourcePath();
			PathWrap b = file.getBackupPath();

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
				.append(separator).append(separator).append("size: ").append(a.size == 0 ? "0" : utils.bytesToString(a.size)).append('\n')
				.append(separator).append(separator).append("last-modified: ").append(a.lastModified == 0 ? "--" : utils.millsToTimeString(a.lastModified)).append('\n');
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
		private void set(Hyperlink h, PathWrap path, boolean isSource) {
			if(path == null) {
				h.setText("--");
				h.setDisable(true);
				return;
			}
			h.setText(subpath(path, isSource).toString());
			h.setDisable(false);
			h.setUserData(path);
		}
		private Object subpath(PathWrap p, boolean isSource) {
			if(p == null)
				return "--";

			String prefix = isSource ? "%source%\\" : "%target%\\";   
			PathWrap start = isSource ? sourceRoot : targetRoot;

			if(start == null || !p.path().startsWith(start.path()))
				return p;

			return prefix + p.path().subpath(start.path().getNameCount(), p.path().getNameCount());
		}
	}
}