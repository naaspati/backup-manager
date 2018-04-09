package sam.backup.manager.config.view;


import static java.lang.String.valueOf;
import static javafx.application.Platform.runLater;
import static javafx.scene.layout.GridPane.REMAINING;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.hyperlink;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.removeClass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.view.FilesView.FilesViewMode;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FilteredFileTree;
import sam.backup.manager.file.SimpleFileTreeWalker;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.fx.helpers.FxHelpers;
import sam.fx.popup.FxPopupShop;

public class ConfigView extends BorderPane implements IStopStart, ButtonAction, ICanceler, WalkListener {
	private final Config config;
	private final GridPane container = new GridPane();
	private final CustomButton button; 
	private final Text sourceSizeT, targetSizeT, sourceFileCountT; 
	private final Text sourceDirCountT, targetFileCountT, targetDirCountT;
	private final Text backupSizeT, backupFileCountT;

	private final Text bottomText;
	private final IStartOnComplete<ConfigView> startEndAction;
	private final AtomicBoolean cancel = new AtomicBoolean();

	public ConfigView(Config config, IStartOnComplete<ConfigView> startEndAction, Long lastUpdated) {
		this.config = config;
		addClass(this, "config-view");
		addClass(container, "container");
		setContextMenu();

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

		sourceSizeT = text("---");
		sourceFileCountT = text("---");
		sourceDirCountT = text("---"); 

		String st = config.isNoBackupWalk() ? "N/A" : "--";
		targetSizeT = text(st); 
		targetFileCountT = text(st); 
		targetDirCountT = text(st);

		backupSizeT = text("---");
		backupFileCountT = text("---");

		int row = 0;

		container.addRow(row, text("Source: "));
		container.add(hyperlink(config.getSource()), 1, row++, REMAINING, 1);
		container.addRow(row,text("Target: "));
		container.add(hyperlink(config.getTarget()), 1, row++, REMAINING, 1);
		container.addRow(row, text("Last updated: "));
		container.add(text(lastUpdated == null ? "N/A" : millsToTimeString(lastUpdated)), 1, row++, REMAINING, 1);

		Label t = FxHelpers.label("SUMMERY", "summery");
		container.add(t, 0, row++, REMAINING, 2);
		GridPane.setHalignment(t, HPos.CENTER);
		GridPane.setValignment(t, VPos.BOTTOM);
		GridPane.setFillWidth(t, true);
		row+=2;
		container.addRow(row++, new Text(), header("Source"), header("Backup"), header("New/Modified"));
		container.addRow(row++, new Text("size  |"), sourceSizeT, targetSizeT, backupSizeT);
		container.addRow(row++, new Text("files |"), sourceFileCountT, targetFileCountT, backupFileCountT);
		container.addRow(row++, new Text("dirs  |"), sourceDirCountT, targetDirCountT);

		bottomText = new Text();

		if(Files.notExists(config.getSource())) {
			button = null;
			finish("Source not found", true);
		}
		else {
			button = new CustomButton(ButtonType.WALK, this);
			container.add(button, 0, row++, REMAINING, 2);
			row++;
		}

		container.add(bottomText, 1, row++, REMAINING, REMAINING);
	}
	private void setContextMenu() {
		setOnContextMenuRequested(e -> {
			if(filteredFileTree == null)
				return;
			MenuItem setAsLetest = new MenuItem("Set as letest");
			setAsLetest.setOnAction(e_e -> {
				config.getFileTree().forcedMarkUpdated();
				try {
					Utils.saveFiletree(config, true);
					FxPopupShop.showHidePopup("marked as letest", 1500);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
			
			ContextMenu menu = new ContextMenu(setAsLetest);
			menu.show(App.getStage(), e.getScreenX(), e.getScreenY());
		});
	}
	private Node header(String string) {
		return addClass(new Label(string), "text", "header");
	}
	private Text text(String str) {
		return FxHelpers.text(str, "text");
	}
	@Override
	public void handle(ButtonType type) {
		switch (type) {
			case FILES:
				FilesView.open(config, filteredFileTree, FilesViewMode.BACKUP);
				break;
			case WALK:
				button.setType(ButtonType.LOADING);
				startEndAction.start(ConfigView.this);
				button.setType(ButtonType.CANCEL);	
				break;
			case SET_MODIFIED:
				throw new IllegalStateException("not yet implemented");
			default:
				throw new IllegalArgumentException("unknown action: "+type);

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
	@Override
	public void walkFailed(String reason, Throwable e) {
		finish(reason, true);
		if(e != null)
			e.printStackTrace();
	}

	private volatile long sourceSize, targetSize;
	private volatile int sourceFileCount, sourceDirCount, targetFileCount, targetDirCount;

	@Override
	public void onFileFound(FileEntity ft, long size, WalkMode mode) {
		runLater(() -> {
			if(mode == WalkMode.SOURCE) {
				sourceSizeT.setText(bytesToString(sourceSize += size));
				sourceFileCountT.setText(valueOf(++sourceFileCount));
			} else if(mode == WalkMode.BACKUP){
				targetSizeT.setText(bytesToString(targetSize += size));
				targetFileCountT.setText(valueOf(++targetFileCount));
			} else {
				throw new IllegalStateException("invalid walkMode: "+mode);
			}
		});
	}

	@Override
	public void onDirFound(DirEntity ft, WalkMode mode) {
		runLater(() -> {
			if(mode == WalkMode.SOURCE) 
				sourceDirCountT.setText(valueOf(++sourceDirCount));
			else if(mode == WalkMode.BACKUP)
				targetDirCountT.setText(valueOf(++targetDirCount));
			else 
				throw new IllegalStateException("invalid walkMode: "+mode);
		});
	}

	private volatile FilteredFileTree filteredFileTree;

	@Override
	public void walkCompleted() {
		runLater(() -> {
			 filteredFileTree = config.getFileTree().getFilteredView(FileTreeEntity::isBackupable);

			if(filteredFileTree.isEmpty()) {
				finish("Nothing to backup/delete", false);
				startEndAction.onComplete(this);
				return;
			}

			button.setType(ButtonType.FILES);
			
			long[] l = {0,0};
			
			filteredFileTree.walk(new SimpleFileTreeWalker() {
				@Override
				public FileVisitResult file(FileEntity ft) {
					l[0]++;
					l[1] += ft.getSourceSize();
					return FileVisitResult.CONTINUE;
				}
			});

			backupSizeT.setText(bytesToString(l[1]));
			backupFileCountT.setText(String.valueOf(l[0]));
			startEndAction.onComplete(this);
		});

	}
	public void finish(String msg, boolean failed) {
		if(failed) {
			config.isDisabled();
			button.setVisible(false);
		}
		button.setType(ButtonType.FILES);
		removeClass(this, "disable", "completed");
		removeClass(bottomText, "disable-text", "completed-text");
		String s = failed ? "disable" : "completed";
		addClass(this, s);
		addClass(bottomText, s+"-text");
		bottomText.setText(msg);
	}
	public boolean hashBackups() {
		return filteredFileTree != null && !filteredFileTree.isEmpty();
	}
	public FilteredFileTree getFilteredFileTree() {
		return filteredFileTree;
	}
}
