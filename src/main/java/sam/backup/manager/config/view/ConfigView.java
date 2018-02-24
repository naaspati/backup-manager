package sam.backup.manager.config.view;


import static java.lang.String.valueOf;
import static javafx.scene.layout.GridPane.REMAINING;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.hyperlink;
import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.removeClass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.AboutFile;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxHelpers;

public class ConfigView extends BorderPane implements IStopStart, Consumer<ButtonType>, ICanceler {
	private final Config config;
	private final GridPane container = new GridPane();
	private final CustomButton button, allFilesButton, setModified; 
	private final Text sourceSize, targetSize, sourceFileCount; 
	private final Text sourceDirCount, targetFileCount, targetDirCount;
	private final Text backupSize, backupFileCount;
	private int backupFileCountNumber;

	private final Text bottomText;
	private final IStartOnComplete<ConfigView> startEndAction;
	private final AtomicBoolean cancel = new AtomicBoolean();
	private final AtomicReference<Node> filesView = new AtomicReference<Node>();

	public ConfigView(Config config, IStartOnComplete<ConfigView> startEndAction, Long lastUpdated) {
		this.config = config;
		addClass(this, "config-view");
		addClass(container, "container");

		this.startEndAction = startEndAction;

		Label l = FxHelpers.label(String.valueOf(config.getSource()),"title");
		l.setMaxWidth(Double.MAX_VALUE);
		setTop(l);
		l.setOnMouseClicked(e -> {
			if(getCenter() == null)
				setCenter(container);
			else 
				getChildren().remove(container);
		});

		sourceSize = text("---");
		sourceFileCount = text("---");
		sourceDirCount = text("---"); 

		String st = config.isNoBackupWalk() ? "N/A" : "--";
		targetSize = text(st); 
		targetFileCount = text(st); 
		targetDirCount = text(st);

		backupSize = text("---");
		backupFileCount = text("---");

		int row = 0;
		
		container.addRow(row, text("Source: "));
		container.add(hyperlink(config.getSource()), 1, row++, REMAINING, 1);
		container.addRow(row,text("Target: "));
		container.add(hyperlink(config.getTarget()), 1, row++, REMAINING, 1);
		container.addRow(row, text("Last updated: "));
		container.add(text(lastUpdated == null ? "N/A" : Utils.millsToTimeString(lastUpdated)), 1, row++, REMAINING, 1);

		Label t = FxHelpers.label("SUMMERY", "summery");
		container.add(t, 0, row++, REMAINING, 2);
		GridPane.setHalignment(t, HPos.CENTER);
		GridPane.setValignment(t, VPos.BOTTOM);
		GridPane.setFillWidth(t, true);
		row+=2;
		container.addRow(row++, new Text(), header("Source"), header("Backup"), header("New/Modified"));
		container.addRow(row++, new Text("size  |"), sourceSize, targetSize, backupSize);
		container.addRow(row++, new Text("files |"), sourceFileCount, targetFileCount, backupFileCount);
		container.addRow(row++, new Text("dirs  |"), sourceDirCount, targetDirCount);

		bottomText = new Text();

		if(Files.notExists(config.getSource())) {
			button = null;
			allFilesButton = null;
			setModified = null;
			finish("Source not found", true);
		}
		else {
			button = new CustomButton(ButtonType.WALK, this);
			allFilesButton = new CustomButton(ButtonType.ALL_FILES, this);
			allFilesButton.setVisible(false);
			setModified = new CustomButton(ButtonType.SET_MODIFIED, this);
			setModified.setVisible(false);
			container.add(new HBox(2, button, allFilesButton /*TODO, setModified */), 0, row++, REMAINING, 2);
			row++;
		}

		container.add(bottomText, 1, row++, REMAINING, REMAINING);
	}
	private Node header(String string) {
		return addClass(new Label(string), "text", "header");
	}
	private Text text(String str) {
		return FxHelpers.text(str, "text");
	}
	@Override
	public void accept(ButtonType type) {
		if(type == ButtonType.WALK) {
			button.setType(ButtonType.LOADING);
			startEndAction.start(ConfigView.this);
			button.setType(ButtonType.CANCEL);
		} else if(type == ButtonType.CANCEL)
			stop();
		else if(type == ButtonType.FILES || type == ButtonType.ALL_FILES) {
			Stage stg = Utils.showStage(new FilesView(type, filesView, config));
			stg.setOnCloseRequest(e -> filesView.set(null));
		} 
		else if(type == ButtonType.SET_MODIFIED) {
			setModified.setVisible(false);
			config.getFileTree().walk(new FileTreeWalker() {
				@Override
				public FileVisitResult file(FileTree ft, AboutFile source, AboutFile backup) {
					ft.setCopied();
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult dir(FileTree ft, AboutFile source, AboutFile backup) {
					ft.setCopied();
					return FileVisitResult.CONTINUE;
				}
			});
			try {
				Utils.saveFiletree(config);
			} catch (IOException e) {
				Platform.runLater(() -> FxAlert.showErrorDialog(config.getSource(), "Failed to save filetree", e));
			}
		}
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
		startEndAction.start(this);
	}
	public boolean isCompleted() {
		return button == null;
	}
	public Config getConfig() {
		return config;
	}
	public void setError(String msg, Exception e) {
		if(e != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			finish(msg+"\n\n"+sw, true);
		}
		else 
			finish(msg, true);
	}
	public ConfigView setSourceSizeFileCount(long sourceSize, int sourceFileCount) {
		this.sourceSize.setText(bytesToString(sourceSize));
		this.sourceFileCount.setText(valueOf(sourceFileCount));
		return this;
	}
	public ConfigView setTargetSizeFileCount(long targetSize, int targetFileCount) {
		this.targetSize.setText(bytesToString(targetSize));
		this.targetFileCount.setText(valueOf(targetFileCount));
		return this;
	}
	public ConfigView setSourceDirCount(int sourceDirCount) {
		this.sourceDirCount.setText(valueOf(sourceDirCount));
		return this;
	}
	public ConfigView setTargetDirCount(int targetDirCount) {
		this.targetDirCount.setText(valueOf(targetDirCount));
		return this;
	}
	public void updateFileTree() {
		List<FileTree> backup = config.getBackupFiles();

		if(backup.isEmpty()) {
			finish("Nothing to backup", false);
			return;
		}
		backupSize.setText(bytesToString(backup.stream().mapToLong(FileTree::getSourceSize).sum()));
		backupFileCount.setText(valueOf(backup.size()));
		backupFileCountNumber = backup.size();

		button.setType(ButtonType.FILES);
		allFilesButton.setVisible(true);
		setModified.setVisible(true);
		startEndAction.onComplete(this);
	}
	
	public void finish(String msg, boolean failed) {
		if(failed)
			config.isDisabled();
		else {
			allFilesButton.setVisible(true);
			setModified.setVisible(true);
		}

		button.setVisible(false);
		removeClass(this, "disable", "completed");
		removeClass(bottomText, "disable-text", "completed-text");
		String s = failed ? "disable" : "completed";
		addClass(this, s);
		addClass(bottomText, s+"-text");
		bottomText.setText(msg);
	}
	public boolean hashBackups() {
		return backupFileCountNumber != 0;
	}
	@SuppressWarnings("rawtypes")
	public void update() {
		Node n = filesView.get();

		if(n == null)
			return;

		Platform.runLater(() -> {
			if(n instanceof ListView)
				((ListView)n).refresh();
			else if(n instanceof TreeView)
				((TreeView)n).refresh();
		});
	}
	public void setLoading(boolean b) {
		// TODO Auto-generated method stub
		
	}
}
