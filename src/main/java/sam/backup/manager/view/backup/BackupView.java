package sam.backup.manager.view.backup;


import static sam.backup.manager.Utils.bytesToString;
import static sam.backup.manager.Utils.fx;
import static sam.backup.manager.Utils.hyperlink;
import static sam.backup.manager.Utils.millsToTimeString;
import static sam.backup.manager.Utils.showErrorDialog;
import static sam.backup.manager.Utils.writeInTempDir;
import static sam.backup.manager.view.ButtonType.DELETE;
import static sam.backup.manager.view.ButtonType.FILES;
import static sam.backup.manager.view.ButtonType.WALK;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.removeClass;
import static sam.fx.helpers.FxMenu.menuitem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

import org.apache.logging.log4j.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FilteredDir;
import sam.backup.manager.file.api.ForcedMarkable;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.view.Deleter;
import sam.backup.manager.view.FilesView;
import sam.backup.manager.view.FilesViewSelector;
import sam.backup.manager.view.WalkHandler;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.fx.helpers.FxLabel;
import sam.fx.helpers.FxText;
import sam.fx.popup.FxPopupShop;
import sam.nopkg.Junk;
import sam.reference.WeakAndLazy;

class BackupView extends BorderPane implements ButtonAction, WalkListener {
	private static final Logger LOGGER = Utils.getLogger(BackupView.class);

	private final Config config;
	private final VBox container = new VBox(5);
	private final CustomButton files = new CustomButton(FILES, this);
	private final CustomButton delete = new CustomButton(DELETE, this);
	private final CustomButton walk = new CustomButton(WALK, this); 
	private final Text sourceSizeT, targetSizeT, sourceFileCountT; 
	private final Text sourceDirCountT, targetFileCountT, targetDirCountT;
	private final Text backupSizeT, backupFileCountT;
	private final WalkHandler handler;
	private final SimpleObjectProperty<FileTree> currentFileTree = new SimpleObjectProperty<>(); 

	private final Text bottomText;

	public BackupView(Config config, Long lastUpdated) {
		this.config = config;
		addClass(this, "config-view");
		addClass(container, "grid");
		setContextMenu();
		this.handler = Junk.notYetImplemented(); //FIXME

		Label l = FxLabel.label(String.valueOf(config.getSource()),"title");
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

		String st = config.getWalkConfig().walkBackup() ? "--" : "N/A";
		targetSizeT = text(st); 
		targetFileCountT = text(st); 
		targetDirCountT = text(st);

		backupSizeT = text("---");
		backupFileCountT = text("---");

		ObservableList<Node> container = this.container.getChildren();

		container.add(text("Source: "));
		container.addAll(new Text("  "),  hyperlink(config.getSource()));
		container.add(text("Target: "));
		container.addAll(new Text("  "),  hyperlink(config.getBaseTarget()));
		container.add(new HBox(5, text("Last updated: "), text(lastUpdated == null ? "N/A" : millsToTimeString(lastUpdated))));

		Label t = FxLabel.label("SUMMERY", "summery");
		t.setMaxWidth(Double.MAX_VALUE);
		t.setAlignment(Pos.CENTER);
		container.add(t);
		
		TilePane tiles = new TilePane(2, 2,
				new Text(), header("Source"), header("Backup"), header("New/Modified"),
				new Text("size  |"), sourceSizeT, targetSizeT, backupSizeT,
				new Text("files |"), sourceFileCountT, targetFileCountT, backupFileCountT,
				new Text("dirs  |"), sourceDirCountT, targetDirCountT);

		container.add(tiles);

		if(config.getSource().stream().allMatch(p -> p.path() == null || Files.notExists(p.path())))
			finish("Source not found", true);
		else {
			container.add(new HBox(5, walk, files, delete));
			files.setVisible(false);
			delete.setVisible(false);
		}
		
		bottomText = new Text();
		container.add(bottomText);
	}
	private FileTree fileTree() {
		return currentFileTree.get();
	}
	private void setContextMenu() {
		setOnContextMenuRequested(e -> {
			ContextMenu menu = new ContextMenu( 
					menuitem("Set as latest", this::setAsLatestAction, backupFFT.isNull().or(Bindings.createBooleanBinding(() -> fileTree() == null || !(fileTree() instanceof ForcedMarkable), currentFileTree))),
					menuitem("All files", this::allfilesAction)
					// menuitem("clean backup", e1 -> new BackupCleanup(config))
					) ;
			menu.show(Utils.window(), e.getScreenX(), e.getScreenY());
		});
	}
	private FilesView openFilesView(String title, Dir dir, FilesViewSelector selector) {
		// TODO Auto-generated method stub
		Junk.notYetImplemented();
	}
	private void allfilesAction(ActionEvent e) {
		if(backupFFT.get() == null)
			if(!loadFileTree())
				return;
		openFilesView("all files", null, FilesViewSelector.all());
	}
	private void setAsLatestAction(ActionEvent e) {
		((ForcedMarkable)fileTree()).forcedMarkUpdated();
		
		if(Utils.saveFileTree(config))
			FxPopupShop.showHidePopup("marked as letest", 1500);
	};
	private Node header(String string) {
		return addClass(new Label(string), "text", "header");
	}
	private Text text(String str) {
		return FxText.text(str, "text");
	}
	@Override
	public void handle(ButtonType type) {
		FilesView view;
		
		switch (type) {
			case FILES:
				view = openFilesView("select files to backup", backupFFT.get(), FilesViewSelector.backup());
				break;
			case DELETE:
				CustomButton button = new CustomButton(ButtonType.DELETE, e -> deleteAction());
				view = openFilesView("select files to delete", deleteFFT.get(), FilesViewSelector.delete());
				view.setButtons(button);
				break;
			case WALK:
				walk.setType(ButtonType.LOADING);
				handler.start(config, this);
				walk.setType(ButtonType.CANCEL);	
				break;
			case SET_MODIFIED:
				throw new IllegalStateException("not yet implemented");
			default:
				throw new IllegalArgumentException("unknown action: "+type);
		}
	}
	
	private final WeakAndLazy<Deleter> deleter = new WeakAndLazy<>(Deleter::new);
	
	private void deleteAction() {
		writeInTempDir(config, "delete", null, new FileTreeString(deleteFFT.get()), LOGGER);
		Deleter d = deleter.get();
		d.start(deleteFFT.get(), fileTree());
		
		//TODO old method Deleter.process(fileTree(), deleteFFT.get())
		Deleter.process(deleteFFT.get())
		.thenAccept(NULL -> Utils.saveFileTree(config));
	}
	@Override
	public boolean isCancelled() {
		return cancel.get();
	}
	@Override
	public void stop() {
		cancel.set(true);
		walk.setType(ButtonType.WALK);
	}
	@Override
	public void start() {
		if(isCompleted())
			return;

		cancel.set(false);
		walk.setType(ButtonType.CANCEL);
		startEndAction.start(this);
	}
	public boolean isCompleted() {
		return walk != null && walk.isDisable();
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
		fx(() -> {
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
	public void onDirFound(Dir ft, WalkMode mode) {
		fx(() -> {
			if(mode == WalkMode.SOURCE) 
				sourceDirCountT.setText(valueOf(++sourceDirCount));
			else if(mode == WalkMode.BACKUP)
				targetDirCountT.setText(valueOf(++targetDirCount));
			else 
				throw new IllegalStateException("invalid walkMode: "+mode);
		});
	}

	private final SimpleObjectProperty<FilteredDir>  backupFFT = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<FilteredDir>  deleteFFT = new SimpleObjectProperty<>();

	@Override
	public void walkCompleted() {
		FilteredDir backup =  new FilteredDir(fileTree(), f -> f.getStatus().isBackupable());
		FilteredDir delete = !config.getBackupConfig().hardSync() ? null :  new FilteredDir(fileTree(), f -> f.getStatus().isBackupDeletable());

		fx(() -> {
			backupFFT.set(backup);
			deleteFFT.set(delete);
			walk.setVisible(false);
		});

		if(!backup.isEmpty())
			updateBackupCounts(backup);
		else
			finish("Nothing to backup/delete", false);

		if(delete != null && !delete.isEmpty())
			updateDeleteCounts(delete);

		fx(() -> startEndAction.onComplete(this));
	}
	private void updateDeleteCounts(FilteredDir deleteFT) {
		fx(() -> delete.setVisible(true));
	}
	private void updateBackupCounts(FilteredDir backup) {
		fx(() -> files.setVisible(true));

		long[] l = {0,0};
		walk(backup, l);
		fx(() -> {
			backupSizeT.setText(bytesToString(l[1]));
			backupFileCountT.setText(String.valueOf(l[0]));
		});
	}
	private void walk(Dir backup, long[] l) {
		for (FileEntity f : backup) {
			if(f.isDirectory())
				walk((Dir)f, l);
			else {
				l[0]++;
				l[1] += f.getSourceAttrs().size();	
			}
		}
	}
	public void finish(String msg, boolean failed) {
		if(failed) {
			config.isDisabled();
		}
		removeClass(this, "disable", "completed");
		removeClass(bottomText, "disable-text", "completed-text");
		String s = failed ? "disable" : "completed";
		addClass(this, s);
		addClass(bottomText, s+"-text");
		bottomText.setText(msg);
	}
	public boolean hashBackups() {
		return backupFFT.get() != null && !backupFFT.get().isEmpty();
	}
	public FilteredDir getBackupFileTree() {
		return backupFFT.get();
	}
	public FilteredDir getDeleteFileTree() {
		return deleteFFT.get();
	}
	public boolean hashDeleteBackups() {
		return deleteFFT.get() != null && !deleteFFT.get().isEmpty();
	}
	public FilteredDir getDeleteBackups() {
		return deleteFFT.get();
	}
	public boolean loadFileTree() {
		if(fileTree() != null)
			return true;

		if(config.getWalkConfig().getDepth() <= 0) {
			fx(() -> finish("Walk failed: \nbad value for depth: "+config.getWalkConfig().getDepth(), true));
			return false;
		}
		if(fileTree() == null) {
			FileTree ft;
			try {
				ft = Utils.readFiletree(config, TreeType.BACKUP, false);
			} catch (Exception e) {
				showErrorDialog(null, "failed to read TreeFile: ", e);
				LOGGER.error("failed to read TreeFile: ", e);
				return false;
			}
			if(ft == null) {
				try {
					ft = Utils.readFiletree(config, TreeType.BACKUP, true);
				} catch (Exception e) {
					showErrorDialog(null, "failed to read TreeFile: ", e);
					LOGGER.error("failed to read TreeFile: ", e);
					return false;	
				} 
				config.setFileTree(ft);	
			} else
				config.setFileTree(ft);
			return true;
		}
		return false;
	}
}
