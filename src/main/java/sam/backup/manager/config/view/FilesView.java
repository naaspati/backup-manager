package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.backup.manager.extra.Utils.saveToFile;
import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.setClass;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import sam.backup.manager.App;
import sam.backup.manager.extra.Files2;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.console.ansi.ANSI;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxHelpers;
import sam.myutils.fileutils.FilesUtils;
import sam.myutils.myutils.MyUtils;
import sam.string.stringutils.StringUtils;

public class FilesView extends BorderPane {
	public enum FileViewMode {
		DELETE, BACKUP
	}

	private static final String separator = "    ";

	private final TreeView<String> treeView;
	private Unit currentRootItem;	
	private final ToggleButton expandAll = new ToggleButton("Expand All");
	private final SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
	private final SimpleIntegerProperty totalCount = new SimpleIntegerProperty();
	private FileViewMode mode;
	private final Path sourceRoot, targetRoot;

	private final ConfigView configView;
	private final FileTree fileTree;
	private final AboutPane aboutPane = new AboutPane();

	public FilesView(ConfigView view) {
		configView = Objects.requireNonNull(view);
		addClass(this, "files-view");
		sourceRoot = view.getConfig().getSource();
		targetRoot = view.getConfig().getTarget();
		this.fileTree = view.getConfig().getFileTree();

		treeView = new TreeView<>();
		treeView.getSelectionModel()
		.selectedItemProperty()
		.addListener((p, o, n) -> aboutPane.reset(n == null ? null : ((Unit)n).file));

		treeView.setCellFactory(CheckBoxTreeCell.forTreeView());

		expandAll.setVisible(false);
		setClass(expandAll, "expand-toggle");
		expandAll.setOnAction(e -> {
			boolean b = expandAll.isSelected();
			expandAll.setText(b ? "collapse all" : "expand all");
			treeView.getRoot().setExpanded(b);
			expand(b, treeView.getRoot().getChildren());
		});

		aboutPane.setMinWidth(300);
		setCenter(new SplitPane(treeView, aboutPane));
		setTop(top());
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
	private void expand(boolean expand, Collection<TreeItem<String>> root) {
		for (TreeItem<String> item : root) {
			item.setExpanded(true);
			expand(expand, item.getChildren());
		}
	}
	private void setBottom() {
		CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveToFile(fileTree.toTreeString(getFilter()), Paths.get("D:\\Downloads").resolve(sourceRoot.getFileName()+".txt")));
		save.disableProperty().bind(selectedCount.isEqualTo(0));

		if(mode == FileViewMode.DELETE) {
			CustomButton d = new CustomButton(ButtonType.DELETE, b -> new Deleter().delete());
			d.disableProperty().bind(save.disableProperty());
			HBox hb = new HBox(2, d, save);
			hb.setPadding(new Insets(5));
			setBottom(hb);
			return;
		}

		BorderPane.setAlignment(save, Pos.CENTER_RIGHT);
		BorderPane.setMargin(save, new Insets(5));

		setBottom(save);
	}
	private class Deleter {
		private final StringBuilder sb = new StringBuilder("DELETED FILES").append('\n');
		private final Set<DirEntity> dirs = new HashSet<>();
		private int success = 0;
		private int failed = 0;
		private final int count = targetRoot.getNameCount();
		
		private void delete() {
			TextArea ta = new TextArea("Please wait");

			runLater(() -> {
				getChildren().clear();
				setCenter(ta);
			});
			
			MyUtils.runOnDeamonThread(() -> {
				deleteWalk(fileTree);

				sb.append("\n-----------\nDirs Deleted: ")
				.append(dirs.stream().distinct()
				.filter(f -> f.getBackupAttrs().getPath().toFile().delete() && f.getParent().remove(f))
				.count()).append('\n');
				
				String s = sb.toString();

				runLater(() -> ta.setText(s));
				LogManager.getLogger(getClass()).debug(s);
				ANSI.disable(false);
			});
		}
		private void deleteWalk(DirEntity dir) {
			for (FileTreeEntity f : dir) {
				Path p = f.getBackupAttrs().getPath();
				StringUtils.repeat(separator, p.getNameCount() - count, sb).append(f.getfileNameString()).append('\n');
				
				if(f.isDirectory() && ((DirEntity)f).hasDeletable()) 
					deleteWalk(((DirEntity)f));
				 else if(f.isDeletable()) {
					 try {
						Files2.delete(p);
						dirs.add(f.getParent());
						success++;
					} catch (IOException e) {
						StringUtils.repeat(separator, p.getNameCount() - count, sb).append("failed delete: ").append(MyUtils.exceptionToString(e)).append('\n');
						failed++;
					}
					 
				 }
			}
		}
	}
	
	public void setMode(FileViewMode mode) {
		if(this.mode == mode)
			return;

		this.mode = mode;
		changeRoot();
		setBottom();
	}
	private final EnumMap<FileViewMode, State> itemsMap = new EnumMap<>(FileViewMode.class);

	private void changeRoot() {
		expandAll.setVisible(true);

		if(currentRootItem != null)
			itemsMap.get(mode).selected = selectedCount.get();

		State state = itemsMap.get(mode);

		if(state == null) {
			currentRootItem = new Unit(fileTree);
			int total = walk(currentRootItem, fileTree, getFilter());
			state = new State(total, currentRootItem);
			itemsMap.put(mode, state);
		}

		selectedCount.set(state.selected);
		totalCount.set(state.total);
		currentRootItem = state.unit;

		treeView.setRoot(currentRootItem);
	}

	private class State {
		int selected;
		final Unit unit;
		final int total;

		public State(int count, Unit unit) {
			this.selected = count;
			this.total = count;
			this.unit = unit;
		}
	}
	private class Unit extends CheckBoxTreeItem<String> {
		final FileTreeEntity file;
		public Unit(FileTreeEntity f) {
			super(f.getfileNameString(), null, true);
			this.file = f;
			if(!f.isDirectory()) {
				selectedProperty().addListener((p, o, n) -> {
					if(n == null) return;
					if(n) set(file, n);
				});
			}
		}
		public void set(FileTreeEntity f, boolean b) {
			if(mode == FileViewMode.DELETE) f.setDeletable(b);
			else {
				 f.setBackupable(b, f.getBackupReason());
				 if(b) configView.getBackups().add((FileEntity)f);
				 else  configView.getBackups().remove((FileEntity)f);
			};
			selectedCount.set(selectedCount.get() + (b ? 1 : -1));
		}
	} 

	private int walk(Unit parent, DirEntity dir, Predicate<FileTreeEntity> filter) {
		int total = 0;
		for (FileTreeEntity f : dir) {
			if(filter.test(f)) {
				Unit item = new Unit(f);
				parent.getChildren().add(item);
				if(f.isDirectory())
					total += walk(item, (DirEntity)f, filter);
				else
					total++;
			}
		}
		return total;
	}
	private Predicate<FileTreeEntity> getFilter() {
		if(mode == FileViewMode.DELETE) return f -> f.isDirectory() ? ((DirEntity)f).hasDeletable() : f.isDeletable(); 
		else return f -> f.isDirectory() ? ((DirEntity)f).hasBackupable() : f.isBackupable();
	}
	private class AboutPane extends VBox {
		final Text name = new Text();
		final Hyperlink sourceLink = new Hyperlink();
		final Hyperlink trgtLink = new Hyperlink();
		final TextArea about = new TextArea();
		final StringBuilder sb = new StringBuilder();
		final GridPane grid = FxHelpers.gridPane(5);

		AboutPane() {
			super(10);
			this.setId("about-pane");

			EventHandler<ActionEvent> handler = e -> {
				Path p = (Path) ((Hyperlink)e.getSource()).getUserData();
				try {
					FilesUtils.openFileLocationInExplorer(p.toFile());
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

			Path s = file.getSourceAttrs().getPath();
			Path b = file.getBackupAttrs().getPath();

			if(s == null)
				return;

			name.setText(s.getFileName().toString());

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

			fileTree.walk(new FileTreeWalker() {

				@Override
				public FileVisitResult file(FileEntity ft, AttrsKeeper source, AttrsKeeper backup) {
					if(ft != file && name.equals(ft.getFileName()))
						list.add(ft);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult dir(DirEntity ft, AttrsKeeper source, AttrsKeeper backup) {
					return FileVisitResult.CONTINUE;
				}
			});
			if(list.isEmpty())
				sb.append("UNKNOWN\n");
			else {
				sb.append("Possibly moved to: \n");
				for (FileTreeEntity f : list) 
					sb.append(separator).append(subpath(f.getBackupAttrs().getPath(), false)).append('\n');
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