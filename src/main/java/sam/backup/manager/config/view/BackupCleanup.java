package sam.backup.manager.config.view;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FilteredFileTree;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;
import static sam.myutils.MyUtilsExtra.*;

public class BackupCleanup extends Stage implements ICanceler, WalkListener, ButtonAction { 

	private final CustomButton button = new CustomButton(ButtonType.CANCEL);
	private final Text text = new Text();
	private final Text backupCount = new Text();
	private final Text sourceCount = new Text();
	private final VBox root = new VBox(10);
	private FilteredFileTree fileTree;
	private final Config config;

	public BackupCleanup(Config config) {
		super(StageStyle.UTILITY);
		this.config = config;
		initModality(Modality.WINDOW_MODAL);
		initOwner(App.getStage());
		boolean error = false;

		if(config.getFileTree() == null)
			try {
				config.setFileTree(Utils.readFiletree(config, TreeType.BACKUP));
			} catch (IOException e1) {
				e1.printStackTrace();
				text.setText("error\n"+e1);
				error = true;
			}

		setScene(new Scene(root));
		getScene().getStylesheets().addAll(App.getStage().getScene().getStylesheets());
		
		if(error) {
			root.getChildren().add(text);
		} else {
			setOnCloseRequest(e -> e.consume());
			text.setText("Walking...");
			root.getChildren().addAll(text, sourceCount, backupCount, button);
			Utils.run(new WalkTask(config, WalkMode.BOTH, this, this));
		}

		setWidth(200);
		setHeight(200);
		show();
	}

	private volatile boolean cancelled;
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	private IdentityHashMap<FileTreeEntity, Boolean> map;

	@Override
	public void walkCompleted() {
		map = new IdentityHashMap<>();
		fileTree = new FilteredFileTree(config.getFileTree(), ft -> {
			if(ft.isDirectory() && !ft.asDir().isWalked())
				return false;
			if(ft.getSourceAttrs().getCurrent() == null &&  ft.getBackupAttrs().getCurrent() != null) {
				map.put(ft, true);
				return true;
			}
			return false;
		}) ;

		Platform.runLater(() -> {
			button.setType(ButtonType.FILES);
			setCount();
			hideable();
		});
	}

	private void setCount() {
		Map<Boolean, Long> count = map.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.partitioningBy(FileTreeEntity::isDirectory, Collectors.counting()));
		text.setText("To delete:\n  Files: "+nullSafe(count.get(false), 0) +"\n  Dirs: "+nullSafe(count.get(true), 0));
	}

	@Override
	public void walkFailed(String reason, Throwable e) {
		Platform.runLater(() -> {
		removeButton();
		hideable();
		text.setText("FAILED\n"+reason+"\n"+e);
		});
	}

	private void hideable() {
		setOnCloseRequest(null);
	}
	private void removeButton() {
		root.getChildren().remove(button);
	}

	int sf, sd, bf, bd;
	
	@Override
	public void onFileFound(FileEntity ft, long size, WalkMode mode) {
		Platform.runLater(() -> {
		if(mode == WalkMode.SOURCE)
			sourceCount.setText(Utils.format("source -> files %s, dirs: %s", ++sf, sd));
		if(mode == WalkMode.SOURCE)
			backupCount.setText(Utils.format("backup -> files %s, dirs: %s", ++bf, bd));
		});
	}

	@Override
	public void onDirFound(DirEntity ft, WalkMode mode) {
		Platform.runLater(() -> {
		if(mode == WalkMode.SOURCE)
			sourceCount.setText(Utils.format("source -> files %s, dirs: %s", sf, ++sd));
		if(mode == WalkMode.SOURCE)
			backupCount.setText(Utils.format("backup -> files %s, dirs: %s", bf, ++bd));
		});
	}
	@Override
	public void handle(ButtonType type) {

		switch (type) {
			case CANCEL:
				cancelled = true;
				removeButton();
				hideable();	
				break;
			case FILES:
				FilesView.open("select files to delete", config, fileTree, new FilesViewMode() {
					@Override
					public void set(FileTreeEntity entity, boolean value) {
						map.put(entity, value);
					}
					@Override
					public boolean isSelectable() {
						return true;
					}
					@Override
					public boolean get(FileTreeEntity entity) {
						return map.get(entity);
					}
				}, new CustomButton(ButtonType.DELETE, this));
				break;
			case DELETE :
				delete();
			default:
				break;
		}
	}
	private void delete() {

	}
}
