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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Cell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.Main;
import sam.backup.manager.config.Config;
import sam.backup.manager.file.AboutFile;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.console.ansi.ANSI;
import sam.fx.helpers.FxHelpers;
import sam.fx.popup.FxPopupShop;
import sam.myutils.myutils.MyUtils;
import sam.weakstore.WeakStore;

public class FilesView extends Stage {
	public enum FileViewMode {
		DELETE, BACKUP
	}
	private static final Image img = new Image("Checkmark_16px.png");
	private static final WeakStore<ImageView> imageViews = new WeakStore<>(() -> new ImageView(img), true);
	private final BorderPane root = new BorderPane();

	private TreeView<FileTreeEntity> treeView; 
	private ListView<Object> listview;
	private final Config config;
	private ButtonType type;
	private boolean isTreeView = true;
	private final FileViewMode mode;
	private final ToggleButton expandAll = new ToggleButton("Expand All");

	public FilesView(Config config, FileViewMode mode) {
		addClass(root, "files-view");
		this.config = config;
		this.type = null;
		this.mode = mode;

		expandAll.setVisible(false);
		setClass(expandAll, "expand-toggle");
		expandAll.setOnAction(e -> {
			boolean b = expandAll.isSelected();
			expandAll.setText(b ? "collapse all" : "expand all");
			expand(b, treeView.getRoot().getChildren());
		});
		root.setCenter(getTreeView());
		root.setTop(top());
		root.setBottom(bottom());
		Scene scene = new Scene(root);
		scene.getStylesheets().add("style.css");
		initModality(Modality.WINDOW_MODAL);
		initOwner(Main.getStage());
		initStyle(StageStyle.UTILITY);
		setScene(scene);
	}
	private Node top() {
		Label l = new Label(String.valueOf(config.getSource()));
		l.setWrapText(true);
		Text count = new Text("count: "+getFiles().size());
		count.setFill(Color.YELLOWGREEN);

		VBox box = new VBox(2, l, new BorderPane(null, null, count, null, expandAll));
		box.setPadding(new Insets(5));
		return box;
	}
	private void expand(boolean expand, Collection<TreeItem<FileTreeEntity>> root) {
		for (TreeItem<FileTreeEntity> item : root) {
			item.setExpanded(true);
			expand(expand, item.getChildren());
		}
	}
	private Node bottom() {
		CustomButton save = new CustomButton(ButtonType.SAVE, e -> saveToFile(config.getFileTree().toTreeString(getFilter()), Paths.get("D:\\Downloads").resolve(config.getSource().getFileName()+".txt")));

		if(mode == FileViewMode.DELETE) {
			HBox hb = new HBox(2, new CustomButton(ButtonType.DELETE_ALL, this::delete), new CustomButton(ButtonType.DELETE_SELECTED, this::delete), save);
			hb.setPadding(new Insets(5));
			return hb;
		}

		RadioButton allFiles = new RadioButton("Show all files");
		RadioButton backFiles = new RadioButton("Show backup files");
		ToggleGroup filesGrp = FxHelpers.toggleGroup(backFiles, allFiles, backFiles);

		RadioButton listv = new RadioButton("List View");
		RadioButton treev = new RadioButton("Tree View");
		ToggleGroup viewGrp = FxHelpers.toggleGroup(treev, listv, treev);

		filesGrp.selectedToggleProperty().addListener((p, o, n) -> {
			type = n == allFiles ?  ButtonType.ALL_FILES : ButtonType.FILES;
			treeView = null;
			listview = null;
			update();
		});
		viewGrp.selectedToggleProperty().addListener((p, o, n) ->{
			isTreeView = treev == n;
			update();
		});

		VBox vbox = new VBox(2, new HBox(2,backFiles, allFiles), new HBox(2,treev, listv));
		vbox.setStyle("-fx-font-size:0.7em;-fx-padding:5px;");

		BorderPane.setAlignment(save, Pos.CENTER);
		BorderPane.setMargin(save, new Insets(5));

		return new BorderPane(vbox, null, save, null, null);
	}
	private void delete(ButtonType type) {
		List<FileTreeEntity> files;

		if(type == ButtonType.DELETE_SELECTED) {
			List<TreeItem<FileTreeEntity>> temp = treeView.getSelectionModel().getSelectedItems();
			if(temp.isEmpty()) {
				FxPopupShop.showHidePopup("nothing selected", 1500);
				return;
			}
			files = temp.stream().map(TreeItem::getValue).collect(Collectors.toList());
			
			treeView.getSelectionModel().clearSelection();
			temp.forEach(t -> {
				if(t.getChildren() != null)
					t.getParent().getChildren().remove(t);
			});
		} else 
			files = Collections.unmodifiableList(config.getDeleteBackupFilesList());

		Thread thrd = new Thread(() -> {
			Map<Path, List<Path>> map = files.stream()
					.map(FileTreeEntity::getTargetPath)
					.collect(Collectors.groupingBy(Path::getParent));

			boolean b = type == ButtonType.DELETE_ALL;
			ANSI.NO_COLOR = b;
			StringBuilder sb = new StringBuilder(b ? ANSI.createUnColoredBanner("DELETED FILES") : ANSI.createBanner("DELETED FILES")).append('\n');
			TextArea ta = b ? new TextArea("Please wait") : null;
			if(b) {
				Platform.runLater(() -> {
					root.getChildren().clear();
					root.setCenter(ta);
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
		thrd.setDaemon(true);
		thrd.start();
	}

	private void update() {
		root.setCenter(isTreeView ? getTreeView() : getListView());
	}
	private Node getListView() {
		expandAll.setVisible(false);
		if(listview != null) {
			listview.refresh();
			return listview;
		}
		int count = config.getTarget().getNameCount();

		ObservableList<Object> list = FXCollections.observableArrayList();
		getFiles().stream().collect(Collectors.groupingBy(f -> f.getTargetPath().getParent()))
		.forEach((s,t) -> {
			list.add(s.subpath(count, s.getNameCount()));
			list.addAll(t);
		});
		listview = new ListView<>(list);

		listview.getSelectionModel().selectedItemProperty()
		.addListener((p, o, n) -> change(n));

		listview.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		listview.setCellFactory(tree -> new ListCell<Object>() {
			
			@Override
			protected void updateItem(Object obj, boolean empty) {
				super.updateItem(obj, empty);
				if(empty || obj == null) {
					setText(null);
					setGraphic(null);
				} else {
					setText((obj instanceof FileTreeEntity ? "   " : "")+obj);
					if(obj instanceof FileEntity)
						toggleImageView(!((FileTreeEntity)obj).isCopied(), this);
					else
						setGraphic(null);
				}
			}
		});
		return listview;
	}
	private List<FileEntity> getFiles() {
		return mode == FileViewMode.DELETE ?  config.getDeleteBackupFilesList() : config.getBackupFiles();
	}
	private Node getTreeView() {
		expandAll.setVisible(true);
		if(treeView != null) {
			treeView.refresh();
			return treeView;
		}

		TreeItem<FileTreeEntity> item = new TreeItem<>();
		walk(item, config.getFileTree().getChildren(), getFilter());

		treeView = new TreeView<>(item);
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty()
		.addListener((p, o, n) -> change(n == null ? null : n.getValue()));

		treeView.setCellFactory(tree -> new TreeCell<FileTreeEntity>() {

			@Override
			protected void updateItem(FileTreeEntity item, boolean empty) {
				super.updateItem(item, empty);
				boolean b = empty || item == null; 
				setText(b ? null : String.valueOf(item.getFileName()));
				toggleImageView(b || !item.isCopied(), this);
			}
		});
		return treeView;
	}
	private void walk(TreeItem<FileTreeEntity> parent, List<FileTreeEntity> children, Predicate<FileTreeEntity> filter) {
		if(children == null || children.isEmpty())
			return;

		for (FileTreeEntity f : children) {
			if(filter.test(f)) {
				TreeItem<FileTreeEntity> item = new TreeItem<>(f);
				parent.getChildren().add(item);
				if(f.isDirectory())
					walk(item, ((DirEntity)f).getChildren(), filter);
			}
		}
	}
	private Predicate<FileTreeEntity> getFilter() {
		if(mode == FileViewMode.DELETE)
			return FileTreeEntity::isDeleteBackup;
		if(type == ButtonType.ALL_FILES)
			return (p -> true);
		else 
			return (FileTreeEntity::isBackupNeeded);
	}

	String format = "source: %s\r\n" + 
			"target: %s\r\n" +
			"lastmodied: %s\r\n" +
			"\r\n" + 
			"About Source:\r\n" + 
			"   size             %s\r\n" + 
			"   last-modifed     %s (%s)\r\n" + 
			"\r\n" + 
			"About Backup: \r\n" + 
			"   size             %s\r\n" + 
			"   last-modifed     %s (%s)\r\n" + 
			"\r\n" + 
			"copied              %s\r\n" + 
			"backup required     %s";
	private void change(Object item) {
		showDetailsDialog();
		if(item == null || !(item instanceof FileTreeEntity)) {
			aboutRootFileTreeTA.setText(null);
			return;
		}
		FileTreeEntity n = (FileTreeEntity) item;

		if(mode == FileViewMode.DELETE)
			deleteInfo(n);
		else if(n.isDirectory())
			setDirDetails(n);
		else
			setFileDetails(n);
	}

	private void setFileDetails(FileTreeEntity n) {
		FileEntity file = (FileEntity) n;
		
		long bs = 0 ;
		long bl = 0 ;
		if(file.getBackupAboutFile() != null) {
			bs = file.getBackupAboutFile().size;
			bl = file.getBackupAboutFile().modifiedTime;
		}

		aboutRootFileTreeTA.setText(String.format(format,
				file.getSourcePath(),
				(file.getTargetPath() == null ? "--" : file.getTargetPath()),
				millsToTimeString(file.getModifiedTime()),
				bytesToString(file.getSourceSize()),
				millsToTimeString(file.getSourceAboutFile().modifiedTime), file.getSourceAboutFile().modifiedTime,
				(bs == 0 ? "--" : bytesToString(bs)),
				(bl == 0 ? "--" : millsToTimeString(bl)), bl == 0 ? "--" : bl,
						(file.isCopied() ? "Yes" : "No"),
						(file.isBackupNeeded() ? "Yes ("+file.getBackupReason()+")" : "No")
				));
	}
	private void setDirDetails(FileTreeEntity n) {
		aboutRootFileTreeTA.setText(String.format("source: %s\r\n" + 
				"target: %s\r\n" +
				"old lastmodied: %s\r\n" +
				"\r\n" + 
				"About Source:\r\n" + 
				"   new last-modifed     %s (%s)\r\n",
				n.getSourcePath(),
				(n.getTargetPath() == null ? "--" : n.getTargetPath()),
				millsToTimeString(n.getModifiedTime()),
				millsToTimeString(n.getSourceAboutFile().modifiedTime), n.getSourceAboutFile().modifiedTime
				));
	}

	private TextArea aboutRootFileTreeTA;
	private Stage stage;
	private void showDetailsDialog() {
		if(aboutRootFileTreeTA == null ) {
			stage = new Stage(StageStyle.UNIFIED);
			stage.initModality(Modality.NONE);
			stage.initOwner(this);

			aboutRootFileTreeTA = new TextArea();
			aboutRootFileTreeTA.setEditable(false);
			aboutRootFileTreeTA.setPrefColumnCount(12);
			aboutRootFileTreeTA.setFont(Font.font("Consolas"));

			RadioMenuItem item = new RadioMenuItem("wrap text");
			aboutRootFileTreeTA.wrapTextProperty().bind(item.selectedProperty());
			ContextMenu menu = new ContextMenu(item);

			aboutRootFileTreeTA.setContextMenu(menu);

			stage.setScene(new Scene(aboutRootFileTreeTA));
			stage.setX(this.getX() + this.getWidth());
			stage.setWidth(300);
			stage.setHeight(300);
			this.xProperty().addListener((p, o, n) -> stage.setX(n.doubleValue() + this.getWidth()));
			this.yProperty().addListener((p, o, n) -> stage.setY(n.doubleValue()));
		}
		if(!stage.isShowing())
			stage.show();
	}
	private void deleteInfo(FileTreeEntity item) {
		if(item.isDirectory()) {
			aboutRootFileTreeTA.setText("source: "+item.getSourcePath());
			return;
		}

		List<FileTreeEntity> list = new ArrayList<>();
		Path name = item.getFileName();

		config.getFileTree().walk(new FileTreeWalker() {

			@Override
			public FileVisitResult file(FileEntity ft, AboutFile source, AboutFile backup) {
				if(ft != item && name.equals(ft.getFileName()))
					list.add(ft);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult dir(DirEntity ft, AboutFile source, AboutFile backup) {
				return FileVisitResult.CONTINUE;
			}
		});
		aboutRootFileTreeTA.setText("source: "+item.getSourcePath()+
				"\ntarget: "+item.getTargetPath()+
				"\n\nreason: "+(list.isEmpty() ? "UNKNOWN" : list.stream().map(FileTreeEntity::getTargetPath).map(String::valueOf).collect(Collectors.joining("\n   ", "Possibly moved to:\n   ", "")))
				);
	}
	private void toggleImageView(boolean remove, Cell<?> cell) {
		if(remove) {
			Node n = cell.getGraphic();
			if(n != null && n instanceof ImageView) {
				imageViews.add((ImageView)n);
				cell.setGraphic(null);
			}
		} else
			cell.setGraphic(imageViews.get());
	}
}