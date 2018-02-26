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
import sam.backup.manager.file.FileTree;
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

	private TreeView<FileTree> treeView; 
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
	private void expand(boolean expand, Collection<TreeItem<FileTree>> root) {
		for (TreeItem<FileTree> item : root) {
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
		List<FileTree> files = null;

		if(type == ButtonType.DELETE_SELECTED) {
			List<TreeItem<FileTree>> temp = treeView.getSelectionModel().getSelectedItems();
			if(temp.isEmpty()) {
				FxPopupShop.showHidePopup("nothing selected", 1500);
				return;
			}
			temp = new ArrayList<>(temp);
			treeView.getSelectionModel().clearSelection();
			temp.forEach(t -> {
				if(t.getChildren() != null)
					t.getParent().getChildren().remove(t);
			});
			files = temp.stream().map(TreeItem::getValue).collect(Collectors.toList());
		}
		if(type == ButtonType.DELETE_ALL) 
			files = config.getDeleteBackupFilesList();

		if(files == null)
			return;
		
		List<FileTree> files2 = files;

		Thread thrd = new Thread(() -> {
			Map<Path, List<Path>> map = files2.stream()
					.map(FileTree::getTargetPath)
					.collect(Collectors.groupingBy(Path::getParent));

			boolean b = type == ButtonType.DELETE_ALL;
			ANSI.NO_COLOR = b;
			StringBuilder sb = new StringBuilder(b ? ANSI.createUnColoredBanner("DELETED FILES") : ANSI.createBanner("DELETED FILES")).append('\n');
			Runnable settext;
			if(b) {
				TextArea ta = new TextArea();
				settext = () -> Platform.runLater(() -> ta.setText(sb.toString()));

				Platform.runLater(() -> {
					root.getChildren().clear();
					root.setCenter(ta);
				});
			} else {
				System.out.println(sb);
				int index[] = {sb.length()}; 
				settext = () -> {
					System.out.print(sb.substring(index[0]));
					index[0] = sb.length();
				};
			}
			map.forEach((s,t) -> {
				yellow(sb, s).append('\n');
				t.forEach(z -> {
					sb.append("  ").append(z.getFileName());
					try {
						Files.delete(z);
					} catch (IOException e) {
						red(sb, "  failed").append(MyUtils.exceptionToString(e));
					}
					sb.append('\n');
				});
				settext.run();
			});
			long count = map.keySet().stream().sorted(Comparator.comparing(Path::getNameCount).reversed()).map(Path::toFile).filter(File::delete).count();
			sb.append("\n-----------\nDirs Deleted: "+count);
			settext.run();
			ANSI.NO_COLOR = false;
		});
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
					setText((obj instanceof FileTree ? "   " : "")+obj);
					if(obj instanceof FileTree)
						toggleImageView(!((FileTree)obj).isCopied(), this);
					else
						setGraphic(null);
				}
			}
		});
		return listview;
	}
	private List<FileTree> getFiles() {
		return mode == FileViewMode.DELETE ?  config.getDeleteBackupFilesList() : config.getBackupFiles();
	}
	private Node getTreeView() {
		expandAll.setVisible(true);
		if(treeView != null) {
			treeView.refresh();
			return treeView;
		}

		TreeItem<FileTree> item = new TreeItem<>();
		walk(item, config.getFileTree().getChildren(), getFilter());

		treeView = new TreeView<>(item);
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty()
		.addListener((p, o, n) -> change(n == null ? null : n.getValue()));

		treeView.setCellFactory(tree -> new TreeCell<FileTree>() {

			@Override
			protected void updateItem(FileTree item, boolean empty) {
				super.updateItem(item, empty);
				boolean b = empty || item == null; 
				setText(b ? null : String.valueOf(item.getFileName()));
				toggleImageView(b || !item.isCopied(), this);
			}
		});
		return treeView;
	}
	private void walk(TreeItem<FileTree> parent, List<FileTree> children, Predicate<FileTree> filter) {
		if(children == null || children.isEmpty())
			return;

		for (FileTree f : children) {
			if(filter.test(f)) {
				TreeItem<FileTree> item = new TreeItem<>(f);
				parent.getChildren().add(item);
				if(f.isDirectory())
					walk(item, f.getChildren(), filter);
			}
		}
	}
	private Predicate<FileTree> getFilter() {
		if(mode == FileViewMode.DELETE)
			return FileTree::isDeleteBackup;
		if(type == ButtonType.ALL_FILES)
			return (p -> true);
		else 
			return (FileTree::isBackupNeeded);
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
		if(item == null || !(item instanceof FileTree)) {
			aboutFileTreeTA.setText(null);
			return;
		}
		FileTree n = (FileTree) item;

		if(mode == FileViewMode.DELETE) {
			deleteInfo(n);
			return;
		}
		if(n.isDirectory()) {
			aboutFileTreeTA.setText(String.format("source: %s\r\n" + 
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
		else {
			long bs = 0 ;
			long bl = 0 ;
			if(n.getBackupAboutFile() != null) {
				bs = n.getBackupAboutFile().size;
				bl = n.getBackupAboutFile().modifiedTime;
			}

			aboutFileTreeTA.setText(String.format(format,
					n.getSourcePath(),
					(n.getTargetPath() == null ? "--" : n.getTargetPath()),
					millsToTimeString(n.getModifiedTime()),
					bytesToString(n.getSourceSize()),
					millsToTimeString(n.getSourceAboutFile().modifiedTime), n.getSourceAboutFile().modifiedTime,
					(bs == 0 ? "--" : bytesToString(bs)),
					(bl == 0 ? "--" : millsToTimeString(bl)), bl == 0 ? "--" : bl,
							(n.isCopied() ? "Yes" : "No"),
							(n.isBackupNeeded() ? "Yes ("+n.getBackupReason()+")" : "No")
					));
		}
	}

	private TextArea aboutFileTreeTA;
	private Stage stage;
	private void showDetailsDialog() {
		if(aboutFileTreeTA == null ) {
			stage = new Stage(StageStyle.UNIFIED);
			stage.initModality(Modality.NONE);
			stage.initOwner(this);

			aboutFileTreeTA = new TextArea();
			aboutFileTreeTA.setEditable(false);
			aboutFileTreeTA.setPrefColumnCount(12);
			aboutFileTreeTA.setFont(Font.font("Consolas"));

			RadioMenuItem item = new RadioMenuItem("wrap text");
			aboutFileTreeTA.wrapTextProperty().bind(item.selectedProperty());
			ContextMenu menu = new ContextMenu(item);

			aboutFileTreeTA.setContextMenu(menu);

			stage.setScene(new Scene(aboutFileTreeTA));
			stage.setX(this.getX() + this.getWidth());
			stage.setWidth(300);
			stage.setHeight(300);
			this.xProperty().addListener((p, o, n) -> stage.setX(n.doubleValue() + this.getWidth()));
			this.yProperty().addListener((p, o, n) -> stage.setY(n.doubleValue()));
		}
		if(!stage.isShowing())
			stage.show();
	}
	private void deleteInfo(FileTree item) {
		if(item.isDirectory()) {
			aboutFileTreeTA.setText("source: "+item.getSourcePath());
			return;
		}

		List<FileTree> list = new ArrayList<>();
		Path name = item.getFileName();

		config.getFileTree().walk(new FileTreeWalker() {

			@Override
			public FileVisitResult file(FileTree ft, AboutFile source, AboutFile backup) {
				if(ft != item && name.equals(ft.getFileName()))
					list.add(ft);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult dir(FileTree ft, AboutFile source, AboutFile backup) {
				return FileVisitResult.CONTINUE;
			}
		});
		aboutFileTreeTA.setText("source: "+item.getSourcePath()+
				"\ntarget: "+item.getTargetPath()+
				"\n\nreason: "+(list.isEmpty() ? "UNKNOWN" : list.stream().map(FileTree::getTargetPath).map(String::valueOf).collect(Collectors.joining("\n   ", "Possibly moved to:\n   ", "")))
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