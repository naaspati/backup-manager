package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.backup.manager.extra.Utils.saveToFile;
import static sam.console.ansi.ANSI.red;
import static sam.console.ansi.ANSI.yellow;
import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.setClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableSet;
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
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sam.backup.manager.App;
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
import sam.myutils.myutils.MyUtils;
import sam.string.stringutils.StringUtils;

public class FilesView extends BorderPane {
	public enum FileViewMode {
		DELETE, BACKUP
	}

	private final TreeView<String> treeView;
	private Unit currentRootItem;	
	private final TextArea aboutTA;
	private final ToggleButton expandAll = new ToggleButton("Expand All");
	private final SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
	private final SimpleIntegerProperty totalCount = new SimpleIntegerProperty();
	private FileViewMode mode;
	private final Path sourceRoot, targetRoot;

	private final ConfigView configView;
	private final FileTree fileTree;

	public FilesView(ConfigView view) {
		configView = Objects.requireNonNull(view);
		addClass(this, "files-view");
		sourceRoot = view.getConfig().getSource();
		targetRoot = view.getConfig().getTarget();
		this.fileTree = view.getConfig().getFileTree();

		treeView = new TreeView<>();
		treeView.getSelectionModel()
		.selectedItemProperty()
		.addListener((p, o, n) -> change(n == null ? null : ((Unit)n).file));

		treeView.setCellFactory(CheckBoxTreeCell.forTreeView());

		expandAll.setVisible(false);
		setClass(expandAll, "expand-toggle");
		expandAll.setOnAction(e -> {
			boolean b = expandAll.isSelected();
			expandAll.setText(b ? "collapse all" : "expand all");
			treeView.getRoot().setExpanded(b);
			expand(b, treeView.getRoot().getChildren());
		});

		aboutTA = new TextArea();
		aboutTA.setEditable(false);
		aboutTA.setPrefColumnCount(12);
		aboutTA.setFont(Font.font("Consolas"));

		RadioMenuItem item = new RadioMenuItem("wrap text");
		aboutTA.wrapTextProperty().bind(item.selectedProperty());
		ContextMenu menu = new ContextMenu(item);

		aboutTA.setContextMenu(menu);

		setCenter(new SplitPane(treeView, aboutTA));
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
		CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveToFile(fileTree.toTreeString(getFilter(false)), Paths.get("D:\\Downloads").resolve(sourceRoot.getFileName()+".txt")));
		save.disableProperty().bind(mode == FileViewMode.DELETE ? Bindings.isEmpty(deletes) : Bindings.isEmpty(backups));

		if(mode == FileViewMode.DELETE) {
			CustomButton d = new CustomButton(ButtonType.DELETE, this::delete);
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
	private void delete(ButtonType type) {
		MyUtils.runOnDeamonThread(() -> {
			Map<Path, List<Path>> map = backups.stream()
					.map(FileTreeEntity::getBackupAttrs)
					.map(AttrsKeeper::getPath)
					.collect(Collectors.groupingBy(Path::getParent));


			ANSI.disable(true);
			StringBuilder sb = new StringBuilder(ANSI.createBanner("DELETED FILES")).append('\n');
			TextArea ta = new TextArea("Please wait");
			runLater(() -> {
				getChildren().clear();
				setCenter(ta);
			});
			map.forEach((s,t) -> {
				yellow(sb, s).append('\n');
				t.forEach(path -> {
					try {
						if(Files.notExists(path))
							return;
						if(Files.isDirectory(path)) 
							deleteDir(path, sb);
						else {
							Files.delete(path);
							sb.append("  ").append(path.getFileName());
						}
					} catch (IOException e) {
						red(sb, "  failed").append(MyUtils.exceptionToString(e));
					}
					sb.append('\n');
				});
			});
			long count = map.keySet().stream().sorted(Comparator.comparing(Path::getNameCount).reversed()).map(Path::toFile).filter(File::delete).count();
			sb.append("\n-----------\nDirs Deleted: "+count);

			runLater(() -> ta.setText(sb.toString()));
			LogManager.getLogger(getClass()).debug(sb.toString());
			ANSI.disable(false);
		});
	}
	private void deleteDir(Path path, StringBuilder sb) throws IOException {
		int count = path.getNameCount();
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				StringUtils.repeat("  ", dir.getNameCount() - count, sb).append(dir.getFileName()).append('\n');
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				StringUtils.repeat("  ", file.getNameCount() - count, sb).append(file.getFileName()).append('\n');
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	private ObservableSet<FileEntity> backups;
	private ObservableSet<FileTreeEntity> deletes;

	public void setMode(FileViewMode mode) {
		this.mode = mode;
		if(mode == FileViewMode.DELETE)
			deletes = configView.getDeletes();
		else if(mode == FileViewMode.BACKUP)
			backups = configView.getBackups();

		setBottom();
		changeRoot();

		totalCount.set(currentRootItem.getTotalCount());
		selectedCount.bind(Bindings.size(backups));
	}
	private final EnumMap<FileViewMode, Unit> itemsMap = new EnumMap<>(FileViewMode.class);

	private void changeRoot() {
		expandAll.setVisible(true);

		currentRootItem = itemsMap.get(mode);

		if(currentRootItem == null) {
			currentRootItem = new Unit(fileTree);
			walk(currentRootItem, fileTree, getFilter(true));
			itemsMap.put(mode, currentRootItem);
			runLater(() -> currentRootItem.setSelected(true));
		}
		treeView.setRoot(currentRootItem);
	}

	class Unit extends CheckBoxTreeItem<String> {
		final FileTreeEntity file;
		public Unit(FileTreeEntity f) {
			super(f.getfileNameString());
			this.file = f;
			if(!f.isDirectory()) {
				selectedProperty().addListener((p, o, n) -> {
					if(n == null) return;
					if(n) add(file);
					else remove(file);
				});
			}
		}

		public void add(FileTreeEntity ent) {
			if(mode == FileViewMode.DELETE) deletes.add(ent);
			else backups.add((FileEntity) ent);
		}
		public void remove(FileTreeEntity ent) {
			if(mode == FileViewMode.DELETE) deletes.remove(ent);
			else backups.remove(ent);
		}

		public int getTotalCount() {
			if(!file.isDirectory())
				return 1;

			if(getChildren().isEmpty())
				return 0;

			return getChildren().stream().mapToInt(t -> ((Unit)t).getTotalCount()).sum();
		}
	} 

	private void walk(Unit parent, DirEntity dir, Predicate<FileTreeEntity> filter) {
		for (FileTreeEntity f : dir) {
			if(filter.test(f)) {
				Unit item = new Unit(f);
				parent.getChildren().add(item);
				if(f.isDirectory())
					walk(item, (DirEntity)f, filter);
			}
		}
	}
	private Predicate<FileTreeEntity> getFilter(boolean full) {
		if(full) {
			if(mode == FileViewMode.DELETE) return FileTreeEntity::isDeleteFromBackup; 
			else return FileTreeEntity::isBackupNeeded;
		} else {
			if(mode == FileViewMode.DELETE) return ft -> ft.isDeleteFromBackup() && (ft.isDirectory() || backups.contains(ft)); 
			else return ft -> ft.isBackupNeeded() && (ft.isDirectory() || backups.contains(ft));
		}
	}
	private void change(FileTreeEntity fw) {
		if(fw == null) {
			aboutTA.setText(null);
			return;
		}
		setFileDetails(fw);
	}

	private void setFileDetails(FileTreeEntity file) {
		StringBuilder sb = new StringBuilder();

		sb.append("source: ").append(subpath(file.getSourceAttrs().getPath(), true)).append('\n')
		.append("target: ").append(subpath(file.getBackupAttrs().getPath(), false)).append('\n');

		append("About Source: \n", file.getSourceAttrs(), sb);
		append("\nAbout Backup: \n", file.getBackupAttrs(), sb);

		if(file.isDirectory()) {
			aboutTA.setText(sb.toString());	
			return;
		}

		if(file.isBackupNeeded()) {
			sb
			.append("\n\n-----------------------------\nWILL BE ADDED TO BACKUP   (")
			.append("reason: ").append(((FileEntity)file).getReason()).append(" ) \n")
			.append("copied to backup: ").append(file.isCopied() ? "YES" : "NO").append('\n');
		}
		if(file.isDeleteFromBackup()) {
			sb
			.append("\n\n-----------------------------\nWILL BE DELETED\n")
			.append("reason:\n");
			appendDeleteReason(file, sb);
		}
		aboutTA.setText(sb.toString());
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
	private static final char[] separator = {' ', ' ', ' ', ' '};
	private void append(String heading, AttrsKeeper ak, StringBuilder sb) {
		sb.append(heading);
		append("old:\n", ak.getOld(), sb);
		append("new:\n", ak.getCurrent(), sb);
	}
	private void append(String heading, Attrs a, StringBuilder sb) {
		if(a != null && (a.getSize() != 0 || a.getModifiedTime() != 0)) {
			sb.append(separator).append(heading)
			.append(separator).append(separator).append("size: ").append(a.getSize() == 0 ? "0" : bytesToString(a.getSize())).append('\n')
			.append(separator).append(separator).append("last-modified: ").append(a.getModifiedTime() == 0 ? "--" : millsToTimeString(a.getModifiedTime())).append('\n');
		}
	}
	private void appendDeleteReason(FileTreeEntity file, StringBuilder sb) {
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
}