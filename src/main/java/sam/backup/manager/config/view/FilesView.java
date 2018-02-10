package sam.backup.manager.config.view;

import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.fx.helpers.FxHelpers.addClass;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import sam.backup.manager.config.Config;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.weakstore.WeakStore;

public class FilesView extends BorderPane {
	private static final Image img = new Image("Checkmark_16px.png");
	private static final WeakStore<ImageView> imageViews = new WeakStore<>(() -> new ImageView(img), true);
	
	WeakReference<TreeView<FileTree>> treeView; 
	WeakReference<ListView<FileTree>> listview;
	TextArea aboutFileTreeTA = new TextArea();
	SplitPane sp = new SplitPane();
	private final AtomicReference<Node> filesView;
	private final Config config;
	private final boolean allFiles;
	

	public FilesView(ButtonType type, AtomicReference<Node> filesView, Config config) {
		this.config = config;
		this.filesView = filesView;
		this.allFiles = type == ButtonType.ALL_FILES;
		
		addClass(this, "files-view");
		filesView.set(getListView());
		aboutFileTreeTA.setEditable(false);
		aboutFileTreeTA.setPrefColumnCount(12);

		sp.setOrientation(Orientation.VERTICAL);
		sp.setDividerPosition(1, 0.5);
		sp.getItems().addAll(filesView.get(), aboutFileTreeTA);
		setCenter(sp);
		setTop(top(type));
	}
	private Node top(ButtonType type) {
		Label l = new Label(String.valueOf(config.getSource()));
		l.setWrapText(true);
		l.setPadding(new Insets(5));
		
		if(type == ButtonType.ALL_FILES)
			return l;

		return new BorderPane(l, null, button(), null, null);
	}
	private Node button() {
		CustomButton b = new CustomButton(ButtonType.TREE_VIEW);
		b.setEventHandler(t -> {
			boolean list = t == ButtonType.LIST_VIEW;
			filesView.set(list ? getListView() : getTreeView());
			sp.getItems().set(0, filesView.get());
			b.setType(list ? ButtonType.TREE_VIEW : ButtonType.LIST_VIEW);
		});
		HBox box = new HBox(b);
		box.setAlignment(Pos.CENTER_RIGHT);
		box.setPadding(new Insets(5));

		return box;
	}
	/** 
	 * dont run with Platoform.runlater 
	 * 
	 */
	private Node getListView() {
		ListView<FileTree> ta = listview == null ? null : listview.get();
		if(ta != null) {
			ta.refresh();
			return ta;
		}

		int count = config.getSource().getNameCount();

		ta = new ListView<>();

		ta.getSelectionModel().selectedItemProperty()
		.addListener((p, o, n) -> change(n));

		ta.getItems().setAll(config.getBackupFiles());
		ta.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		ta.setCellFactory(tree -> new ListCell<FileTree>() {

			@Override
			protected void updateItem(FileTree item, boolean empty) {
				super.updateItem(item, empty);
				boolean b = empty || item == null; 
				setText(b ? null : item.getSourcePath().subpath(count, item.getSourcePath().getNameCount()).toString());
				toggleImageView(b || !item.isCopied(), this);
			}
		});
		listview = new WeakReference<>(ta);
		return ta;
	}

	private Node getTreeView() {
		TreeView<FileTree> tv = treeView == null ? null : treeView.get();
		if(tv != null) {
			tv.refresh();
			return tv;
		}

		TreeItem<FileTree> item = new TreeItem<>();
		walk(item, config.getFileTree().getChildren());

		tv = new TreeView<>(item);
		tv.setShowRoot(false);
		tv.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tv.getSelectionModel().selectedItemProperty()
		.addListener((p, o, n) -> change(n == null ? null : n.getValue()));

		tv.setCellFactory(tree -> new TreeCell<FileTree>() {

			@Override
			protected void updateItem(FileTree item, boolean empty) {
				super.updateItem(item, empty);
				boolean b = empty || item == null; 
				setText(b ? null : String.valueOf(item.getFileName()));
				toggleImageView(b || !item.isCopied(), this);
			}
		});
		treeView = new WeakReference<TreeView<FileTree>>(tv);
		return tv;
	}
	private void walk(TreeItem<FileTree> parent, List<FileTree> children) {
		if(children == null || children.isEmpty())
			return;

		for (FileTree f : children) {
			if(!allFiles && !f.isBackupNeeded())
				continue;
			TreeItem<FileTree> item = new TreeItem<>(f);
			parent.getChildren().add(item);
			if(f.isDirectory())
				walk(item, f.getChildren());
		}
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
	public void change(FileTree n) {
		if(n == null)
			aboutFileTreeTA.setText(null);
		else if(n.isDirectory()) {
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
	
	private void toggleImageView(boolean remove, Cell<FileTree> cell) {
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