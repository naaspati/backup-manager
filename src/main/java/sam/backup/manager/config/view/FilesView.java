package sam.backup.manager.config.view;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sam.backup.manager.config.Config;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.console.ansi.ANSI;
import sam.myutils.myutils.MyUtils;

public class FilesView extends BorderPane {
	public enum FileViewMode {
		DELETE, BACKUP
	}

	private final TreeView<String> treeView;
	private Unit currentRootItem;	
	private final TextArea aboutTA;
	private final Config config;
	private final ToggleButton expandAll = new ToggleButton("Expand All");
	private final SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
	private final SimpleIntegerProperty totalCount = new SimpleIntegerProperty();
	private FileViewMode mode;
	private final ObservableWalkResult result;

	public FilesView(Config config, ObservableWalkResult result) {
		Objects.requireNonNull(result);
		addClass(this, "files-view");
		this.config = config;
		this.result = result;


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
		Label l = new Label(String.valueOf(config.getSource()));
		l.setWrapText(true);
		Text count = new Text();
		count.textProperty().bind(Bindings.concat("selected/total: ", selectedCount, "/", totalCount));
		count.setFill(Color.YELLOWGREEN);

		VBox box = new VBox(2, l, new BorderPane(null, null, count, null, expandAll));
		box.setPadding(new Insets(5));
		return box;
	}
	private void expand(boolean expand, Collection<TreeItem<String>> root) {
		for (TreeItem<String> item : root) {
			item.setExpanded(true);
			expand(expand, item.getChildren());
		}
	}
	private void setBottom() {
		CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveToFile(config.getFileTree().toTreeString(getFilter(false)), Paths.get("D:\\Downloads").resolve(config.getSource().getFileName()+".txt")));
		save.disableProperty().bind(Bindings.isEmpty(currentFiles));

		if(mode == FileViewMode.DELETE) {
			CustomButton d = new CustomButton(ButtonType.DELETE, this::delete);
			d.disableProperty().bind(save.disableProperty());
			HBox hb = new HBox(2, d, save);
			hb.setPadding(new Insets(5));
			setBottom(hb);
			return;
		}

		BorderPane.setAlignment(save, Pos.CENTER);
		BorderPane.setMargin(save, new Insets(5));

		setBottom(save);
	}
	private void delete(ButtonType type) {
		MyUtils.runOnDeamonThread(() -> {
			Map<Path, List<Path>> map = currentFiles.stream()
					.map(FileTreeEntity::getTargetPath)
					.collect(Collectors.groupingBy(Path::getParent));


			boolean b = mode == FileViewMode.DELETE;
			ANSI.NO_COLOR = b;
			StringBuilder sb = new StringBuilder(b ? ANSI.createUnColoredBanner("DELETED FILES") : ANSI.createBanner("DELETED FILES")).append('\n');
			TextArea ta = b ? new TextArea("Please wait") : null;
			if(b) {
				Platform.runLater(() -> {
					getChildren().clear();
					setCenter(ta);
				});
			}

			map.forEach((s,t) -> {
				yellow(sb, s).append('\n');
				t.forEach(z -> {
					sb.append("  ").append(z.getFileName());
					try {
						Files.deleteIfExists(z);
					} catch (IOException e) {
						red(sb, "  failed").append(MyUtils.exceptionToString(e));
					}
					sb.append('\n');
				});
			});
			long count = map.keySet().stream().sorted(Comparator.comparing(Path::getNameCount).reversed()).map(Path::toFile).filter(File::delete).count();
			sb.append("\n-----------\nDirs Deleted: "+count);

			if(b) 
				Platform.runLater(() -> ta.setText(sb.toString()));
			else 
				System.out.println(sb);

			ANSI.NO_COLOR = false;
		});
	}
	private ObservableSet<FileEntity> currentFiles;

	public void setMode(FileViewMode mode) {
		this.mode = mode;
		this.currentFiles = mode == FileViewMode.DELETE ? result.getDeletes() : result.getBackups();

		setBottom();
		changeRoot();

		totalCount.set(currentRootItem.getTotalCount());
		selectedCount.bind(Bindings.size(currentFiles));
	}
	private final EnumMap<FileViewMode, Unit> itemsMap = new EnumMap<>(FileViewMode.class);

	private void changeRoot() {
		expandAll.setVisible(true);

		currentRootItem = itemsMap.get(mode);

		if(currentRootItem == null) {
			currentRootItem = new Unit(config.getFileTree());
			walk(currentRootItem, config.getFileTree(), getFilter(true));
			itemsMap.put(mode, currentRootItem);
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
					if(n) currentFiles.add((FileEntity)file);
					else currentFiles.remove(file);
				});
			}
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
			if(mode == FileViewMode.DELETE) return ft -> ft.isDeleteFromBackup() && (ft.isDirectory() || currentFiles.contains(ft)); 
			else return ft -> ft.isBackupNeeded() && (ft.isDirectory() || currentFiles.contains(ft));
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

		sb.append("source: ").append(file.getSourcePath()).append('\n')
		.append("target: ").append(file.getTargetPath() == null ? "--" : file.getTargetPath()).append('\n');

		append("About Source: \n", file.getSourceAttrs(), sb);
		append("\nAbout Backup: \n", file.getBackupAttrs(), sb);

		if(file.isDirectory()) {
			aboutTA.setText(sb.toString());	
			return;
		}

		if(file.isBackupNeeded()) {
			sb.append('\n')
			.append("WILL NE ADDED TO BACKUP \n")
			.append(separator).append("reason: ").append(((FileEntity)file).getReason()).append('\n')
			.append("copied to backup: ").append(file.isCopied() ? "YES" : "NO").append('\n');
		}
		if(file.isDeleteFromBackup()) {
			sb.append('\n')
			.append("WILL BE DELETED\n")
			.append("reason:\n");
			appendDeleteReason(file, sb);
		}
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

		config.getFileTree().walk(new FileTreeWalker() {

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
			final int c = config.getTarget().getNameCount();
			sb.append("Possibly moved to: \n");
			for (FileTreeEntity f : list) {
				Path p2 = f.getTargetPath();
				sb.append(separator).append(p2.subpath(c, p2.getNameCount())).append('\n');
			}
		}
	}
}