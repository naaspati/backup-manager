package sam.backup.manager.view;

import static java.lang.String.valueOf;
import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.fx.helpers.FxHelpers.addClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;
import sam.fx.helpers.FxHelpers;

public class ConfigView extends TitledPane implements IStopStart, Consumer<ButtonType>, ICanceler {
	private final Config config;
	private CustomButton button; 
	private final GridPane gp = new GridPane();
	private ScrollPane scrollPane;
	private Text sourceSize, targetSize, sourceFileCount; 
	private Text sourceDirCount, targetFileCount, targetDirCount;
	private Text backupSize, backupFileCount;

	private List<FileTreeView> backupFiles;
	private boolean sourceWalkCompleted;
	private final Consumer<ConfigView> walkCompleteAction;
	private final Consumer<ConfigView> walkAction;

	private final AtomicBoolean cancel = new AtomicBoolean();

	public ConfigView(Config config, Consumer<ConfigView> walkAction, Consumer<ConfigView> walkCompleteAction, Long lastUpdated) {
		this.config = config;
		addClass(this, "config-view", "white-back");
		setText(config.getSource().toString());
		this.walkCompleteAction = walkCompleteAction;
		this.walkAction = walkAction;

		gp.setHgap(5);
		gp.setVgap(2);

		gp.setMaxWidth(Double.MAX_VALUE);
		addClass(gp, "container");

		button = new CustomButton(ButtonType.WALK, this);
		setContent(gp);

		gp.addRow(0, text("Source "), text(String.valueOf(config.getSource())));

		if(Files.notExists(config.getSource())) {
			disable();
			return;
		}
		gp.addRow(1, text("Target "), text(String.valueOf(config.getTarget())));
		gp.addRow(2, text("Last updated "), text(Utils.millsToTimeString(lastUpdated)));
		gp.add(button, 0, lastUpdated != null ? 4 : 3, GridPane.REMAINING, 1);
	}
	private Text text(String str) {
		return FxHelpers.text(str, "text");
	}
	@Override
	public void accept(ButtonType type) {
		if(type == ButtonType.WALK)
			runLater(this::fillView);

		button.setType(ButtonType.CANCEL);
		walkAction.accept(ConfigView.this);
	}
	private void fillView() {
		gp.getChildren().clear();

		sourceSize = text("--");
		targetSize = text("--"); 
		sourceFileCount = text("--");
		sourceDirCount = text("--"); 
		targetFileCount = text("--"); 
		targetDirCount = text("--");

		backupSize = text("--");
		backupFileCount = text("--");

		setContent(null);
		scrollPane = new ScrollPane(gp);
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		setContent(scrollPane);

		int row = 0;
		gp.add(button, 0, row++, GridPane.REMAINING, 1);
		gp.addRow(row++, text("Source: "));
		add(row++, "Path: ", text(String.valueOf(config.getSource())));

		if(Files.notExists(config.getSource())) {
			disable();
			return;
		}

		add(row++, "Size: ", sourceSize);
		add(row++, "Files: ", sourceFileCount);
		add(row++, "Folders: ", sourceDirCount);

		gp.addRow(row++, text("Target: "));
		add(row++, "Path: ", text(String.valueOf(config.getTarget())));
		add(row++, "Size: ", targetSize);
		add(row++, "Files: ", targetFileCount);
		add(row++, "Folders: ", targetDirCount);
	}
	@Override
	public boolean isCancelled() {
		return cancel.get();
	}
	@Override
	public void stop() {
		cancel.set(true);
		button.setType(ButtonType.WALK);
	}
	@Override
	public void start() {
		if(isCompleted())
			return;

		cancel.set(false);
		button.setType(ButtonType.CANCEL);
		walkAction.accept(this);
	}
	public boolean isCompleted() {
		return button == null;
	}
	private void add(int row, String string, Text textNode) {
		gp.add(text(string), 1, row, 1, 1);
		gp.add(textNode, 2, row, GridPane.REMAINING, 1);
		GridPane.setHgrow(textNode, Priority.ALWAYS);
	}
	public Config getConfig() {
		return config;
	}
	public void setError(String msg, Exception e) {
		Text t;
		gp.add(t = text(msg), 0, gp.getChildren().size(), GridPane.REMAINING, 2);
		addClass(t, "source-not-found-text");

		if(e != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			TextArea ta = new TextArea(sw.toString());
			GridPane.setHgrow(ta, Priority.ALWAYS);
			gp.add(ta, 0, gp.getChildren().size(), GridPane.REMAINING, GridPane.REMAINING);
		}
	}
	public void setSourceSizeFileCount(long sourceSize, int sourceFileCount) {
		this.sourceSize.setText(bytesToString(sourceSize));
		this.sourceFileCount.setText(valueOf(sourceFileCount));
	}
	public void setTargetSizeFileCount(long targetSize, int targetFileCount) {
		this.targetSize.setText(bytesToString(targetSize));
		this.targetFileCount.setText(valueOf(targetFileCount));
	}
	public void setSourceDirCount(int sourceDirCount) {
		this.sourceDirCount.setText(valueOf(sourceDirCount));
	}
	public void setTargetDirCount(int targetDirCount) {
		this.targetDirCount.setText(valueOf(targetDirCount));
	}
	public void setFileTree(FileTree tree, boolean onlyExistsCheck) {
		gp.getChildren().removeIf(n -> n instanceof HBox);

		int row = gp.getChildren().size();

		FileTreeView view = new FileTreeView(tree, onlyExistsCheck);
		tree.calculateTargetPath(config);

		Count c = new Count();
		walk(view, c);

		if(c.fileCount == 0) {
			setCompleted();
			return;
		}

		backupSize.setText(bytesToString(c.size));
		backupFileCount.setText(valueOf(c.fileCount));

		gp.addRow(row++, text("Backup: "));
		add(row++, "Size: ", backupSize);
		add(row++, "Files: ", backupFileCount);

		BorderPane root = new BorderPane();

		Button btn = new Button("Tree View");
		root.setCenter(getListView(backupFiles, config.getSource()));

		btn.setOnAction(e -> {
			if(btn.getText().equals("Tree View")) {
				btn.setText("List View");
				root.setCenter(getTreeView(view));
			} else {
				btn.setText("Tree View");
				root.setCenter(getListView(backupFiles, config.getSource()));
			}
		});

		gp.add(btn, 2, row++, GridPane.REMAINING, 1);
		gp.add(root, 2, row++, GridPane.REMAINING, GridPane.REMAINING);
		gp.getChildren().remove(button);
		button = null;
		walkCompleteAction.accept(this);
	}

	private void setCompleted() {
		addClass(this, "no-backup");
		Text t = text("Backup Up-To-Date");
		GridPane.setHalignment(t, HPos.CENTER);
		t.setTextAlignment(TextAlignment.CENTER);
		addClass(t, "no-backup-text");
		gp.add(t, 0, gp.getChildren().size(), GridPane.REMAINING, 1);
		((Pane)button.getParent()).getChildren().remove(button);
	}

	private Text listview;
	private Node getListView(List<FileTreeView> list, Path root) {
		if(listview != null) return listview;
		int c = root.getNameCount();

		listview = new Text(list.stream()
				.map(FileTreeView::getFileTree)
				.map(FileTree::getSourcePath)
				.map(p -> p.subpath(c, p.getNameCount()))
				.map(String::valueOf)
				.collect(Collectors.joining("\n ", root+"\n ", "")));

		return listview;
	}

	private TreeView<FileTree> treeView; 
	private Node getTreeView(FileTreeView view) {
		if(treeView != null) return treeView;

		treeView = new TreeView<>(view);
		Image tickImg = new Image("Checkmark_16px.png");
		treeView.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(treeView, Priority.ALWAYS);
		treeView.setMinHeight(200);
		treeView.setCellFactory(item -> new TreeCell<FileTree>() {
			@Override
			protected void updateItem(FileTree item, boolean empty) {
				super.updateItem(item, empty);
				if(empty) {
					setGraphic(null);
					setText(null);
					return;
				}
				if(!item.isDirectory() && item.isCopied())
					setGraphic(new ImageView(tickImg));
				else
					setGraphic(null);

				setText(item.toString());
			}

		});
		treeView.setMaxHeight(Double.MAX_VALUE);
		return treeView;
	}
	private void walk(FileTreeView view,Count c) {
		for (TreeItem<FileTree> t : view.getChildren()) {
			FileTreeView v = (FileTreeView)t;
			if(v.getFileTree().isDirectory())
				walk(v, c);
			else {
				c.fileCount++;
				c.size += v.getFileTree().getSourceSize();
				if(backupFiles == null)
					backupFiles = new ArrayList<>();
				backupFiles.add(v);
			}
		}
	}
	public void disable() {
		if(config.isDisabled())
			return;

		config.disable();
		gp.getChildren().remove(button);
		addClass(this, "source-not-exists");
		Text t = text("Source Not Found");
		GridPane.setHalignment(t, HPos.CENTER);
		t.setTextAlignment(TextAlignment.CENTER);
		addClass(t, "source-not-found-text");
		gp.add(t, 0, gp.getChildren().size(), GridPane.REMAINING, 1);
	}

	public List<FileTreeView> getBackupFiles() {
		return backupFiles;
	}
	private class Count {
		long size;
		int fileCount;
	}
	public void setSourceWalkCompleted() {
		sourceWalkCompleted = true;
	}
	public boolean isSourceCompleted() {
		return sourceWalkCompleted;
	}
}
