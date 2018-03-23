package sam.backup.manager.config.view;


import static java.lang.String.valueOf;
import static javafx.application.Platform.runLater;
import static javafx.scene.layout.GridPane.REMAINING;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.hyperlink;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.removeClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.view.FilesView.FileViewMode;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.file.AttrsKeeper;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.fx.helpers.FxHelpers;

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
				filesVies(FileViewMode.BACKUP);
				break;
			case DELETE:
				filesVies(FileViewMode.DELETE);
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

	private FilesView fv;
	private Stage stage;
	private void filesVies(FileViewMode mode) {
		if(fv == null) {
			fv = new FilesView(this);

			stage = new Stage();
			Scene scene = new Scene(fv);
			scene.getStylesheets().add("styles.css");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(App.getStage());
			stage.initStyle(StageStyle.UTILITY);
			stage.setScene(scene);
			stage.setWidth(600);
		}
		fv.setMode(mode);
		stage.show();
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
	
	private volatile ObservableSet<FileEntity> backups;
	public ObservableSet<FileEntity> getBackups() {
		return backups;
	}
	@Override
	public void walkCompleted() {
		runLater(() -> {
			int[] deleteCount = {0};
			
			HashSet<FileEntity> backups = new HashSet<>(); 

			FileTreeWalker ftw = new FileTreeWalker() {
				@Override
				public FileVisitResult file(FileEntity ft, AttrsKeeper source, AttrsKeeper backup) {
					if(ft.isBackupable()) 
						backups.add(ft);
					else if(ft.isDeletable())
						deleteCount[0]++;

					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult dir(DirEntity ft, AttrsKeeper source, AttrsKeeper backup) {
					return FileVisitResult.CONTINUE;
				}
			};

			config.getFileTree().walk(ftw);

			if(backups.isEmpty() && deleteCount[0] == 0) {
				finish("Nothing to backup/delete", false);
				startEndAction.onComplete(this);
				return;
			}
			
			if(!backups.isEmpty() && deleteCount[0] != 0) {
				button.setType(ButtonType.FILES);
				int c = GridPane.getColumnIndex(button);
				int r = GridPane.getRowIndex(button);
				container.getChildren().remove(button);
				HBox hb = new HBox(2, button, new CustomButton(ButtonType.DELETE, this));
				container.add(hb, c, r, GridPane.REMAINING, GridPane.REMAINING);	
			} else 
				button.setType(backups.isEmpty() ? ButtonType.DELETE : ButtonType.FILES);

			backupSizeT.setText(bytesToString(backups.stream().mapToLong(b -> b.getSourceAttrs().getSize()).sum()));
			backupFileCountT.setText(String.valueOf(backups.size()));
			this.backups = FXCollections.observableSet(backups);
		});
		startEndAction.onComplete(this);
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
		return backups != null && !backups.isEmpty();
	}
}
