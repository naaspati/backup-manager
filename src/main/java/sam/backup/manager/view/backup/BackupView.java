package sam.backup.manager.view.backup;

import static java.lang.String.valueOf;
import static sam.backup.manager.Utils.bytesToString;
import static sam.backup.manager.Utils.millsToTimeString;
import static sam.backup.manager.Utils.writeInTempDir;
import static sam.backup.manager.UtilsFx.fx;
import static sam.backup.manager.UtilsFx.hyperlink;
import static sam.backup.manager.view.ButtonType.DELETE;
import static sam.backup.manager.view.ButtonType.FILES;
import static sam.backup.manager.view.ButtonType.WALK;
import static sam.fx.alert.FxAlert.showErrorDialog;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.removeClass;
import static sam.fx.helpers.FxMenu.menuitem;

import java.nio.file.Path;
import java.util.List;

import javax.inject.Provider;

import org.apache.logging.log4j.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.file.api.FilteredDir;
import sam.backup.manager.file.api.ForcedMarkable;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.view.Deleter;
import sam.backup.manager.view.FilesView;
import sam.backup.manager.view.FilesViewSelector;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.fx.helpers.FxLabel;
import sam.fx.helpers.FxText;
import sam.nopkg.Junk;

class BackupView extends BorderPane {
	private final Logger LOGGER = Utils.getLogger(BackupView.class);

	private final Config config;
	private final SimpleObjectProperty<FileTree> currentFileTree = new SimpleObjectProperty<>();
	private final Provider<Deleter> deleter;
	private final FileTreeManager factory;
	private final Node root;
	
	public BackupView(Config config, FileTreeManager factory, Provider<Deleter> deleter) {
		this.config = config;
		this.deleter = deleter;
		this.factory = factory;
		
		addClass(this, "config-view");
		
		List<FileTreeMeta> metas = config.getFileTreeMetas();
		
		if(metas.isEmpty())
			root = UtilsFx.bigPlaceholder("NO FileTreeMeta Specified");
		else if(metas.size() == 1)
			root = new MetaTabContent(metas.get(0));
		else 
			root = new TabPane(metas.stream().map(MetaTab::new).toArray(Tab[]::new));
		
		Label l = FxLabel.label(config.getName(),"title");
		l.setMaxWidth(Double.MAX_VALUE);
		setTop(l);
		l.setOnMouseClicked(e -> {
			if(getCenter() == null)
				setCenter(root);
			else 
				getChildren().remove(root);
		});
		
		
	}
	
	private class MetaTab extends Tab {
		
		public MetaTab(FileTreeMeta ft) {
			getStyleClass().add("meta-tab");
			setText(title(ft));
			setContent(new MetaTabContent(ft));
		}

		private String title(FileTreeMeta ft) {
			return ft.toString(); //FIXME
		}
	}
	private class MetaTabContent extends VBox implements ButtonAction, WalkListener  {
		final FileTreeMeta meta;
		final Text bottomText;
		
		private final CustomButton files = new CustomButton(FILES, this);
		private final CustomButton delete = new CustomButton(DELETE, this);
		private final CustomButton walk = new CustomButton(WALK, this); 
		
		private final Text sourceSizeT, targetSizeT, sourceFileCountT; 
		private final Text sourceDirCountT, targetFileCountT, targetDirCountT;
		private final Text backupSizeT, backupFileCountT;
		
		private final SimpleObjectProperty<FilteredDir>  backupFFT = new SimpleObjectProperty<>();
		private final SimpleObjectProperty<FilteredDir>  deleteFFT = new SimpleObjectProperty<>();

		public MetaTabContent(FileTreeMeta ft) {
			super(5);
			this.meta = ft;
			
			addClass(this, "meta-content");
			setContextMenu();
			
			
			ObservableList<Node> list = getChildren();
			PathWrap source = ft.getSource();
			PathWrap target = ft.getTarget();

			list.add(text("Source: "));
			list.addAll(new Text("  "),  hyperlink(source));
			list.add(text("Target: "));
			list.addAll(new Text("  "),  hyperlink(target));
			long lastUpdated = ft.getLastModified();
			list.add(new HBox(5, text("Last updated: "), text(lastUpdated <= 0 ? "N/A" : millsToTimeString(lastUpdated))));

			Label t = FxLabel.label("SUMMERY", "summery");
			t.setMaxWidth(Double.MAX_VALUE);
			t.setAlignment(Pos.CENTER);
			list.add(t);
			
			sourceSizeT = text("---");
			sourceFileCountT = text("---");
			sourceDirCountT = text("---"); 

			String st = config.getWalkConfig().walkBackup() ? "--" : "N/A";
			targetSizeT = text(st); 
			targetFileCountT = text(st); 
			targetDirCountT = text(st);

			backupSizeT = text("---");
			backupFileCountT = text("---");
			
			TilePane tiles = new TilePane(2, 2,
					new Text(), header("Source"), header("Backup"), header("New/Modified"),
					new Text("size  |"), sourceSizeT, targetSizeT, backupSizeT,
					new Text("files |"), sourceFileCountT, targetFileCountT, backupFileCountT,
					new Text("dirs  |"), sourceDirCountT, targetDirCountT);

			list.add(tiles);

			if(ft.getSource() == null || !ft.getSource().exists())
				finish(this, "Source not found", true);
			else {
				list.add(new HBox(5, walk, files, delete));
				files.setVisible(false);
				delete.setVisible(false);
			}
			
			bottomText = new Text();
			list.add(bottomText);
		}
		
		@Override
		public void handle(ButtonType type) {
			FilesView view;
			
			switch (type) {
				case FILES:
					view = openFilesView("select files to backup", backupFFT.get(), FilesViewSelector.backup());
					break;
				case DELETE:
					view = openFilesView("select files to delete", deleteFFT.get(), FilesViewSelector.delete());
					view.setButtons(new CustomButton(ButtonType.DELETE, e -> deleteAction()));
					break;
				case WALK:
					walk.setType(ButtonType.LOADING);
					//FIXME handler.start(config, this);
					walk.setType(ButtonType.CANCEL);	
					break;
				case SET_MODIFIED:
					throw new IllegalStateException("not yet implemented");
				default:
					throw new IllegalArgumentException("unknown action: "+type);
			}
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
				fx(() -> finish(this, "Walk failed: \nbad value for depth: "+config.getWalkConfig().getDepth(), true));
				return false;
			}
			if(fileTree() == null) {
				try {
					if(meta.loadFiletree(factory, false) == null)
						meta.loadFiletree(factory, true);
				} catch (Exception e) {
					showErrorDialog(null, "failed to read TreeFile: ", e);
					LOGGER.error("failed to read TreeFile: ", e);
					return false;
				}
				
				return true;
			}
			return false;
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

		private void updateDeleteCounts(FilteredDir deleteFT) {
			fx(() -> delete.setVisible(true));
		}
		private void updateBackupCounts(FilteredDir backup) {
			fx(() -> files.setVisible(true));

			long[] l = {0,0};
			walk(backup, l);
			fx(() -> {
				backupSizeT.setText(bytesToString(l[1]));
				backupFileCountT.setText(valueOf(l[0]));
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
		
		private void allfilesAction(ActionEvent e) {
			if(backupFFT.get() == null)
				if(!loadFileTree())
					return;
			openFilesView("all files", null, FilesViewSelector.all());
		}
		private void setAsLatestAction(ActionEvent e) {
			((ForcedMarkable)fileTree()).forcedMarkUpdated();
		};
		
		private void setContextMenu() {
			setOnContextMenuRequested(e -> {
				ContextMenu menu = new ContextMenu( 
						menuitem("Set as latest", this::setAsLatestAction, backupFFT.isNull().or(Bindings.createBooleanBinding(() -> fileTree() == null || !(fileTree() instanceof ForcedMarkable), currentFileTree))),
						menuitem("All files", this::allfilesAction)
						) ;
				menu.show(this, e.getScreenX(), e.getScreenY());
			});
		}
		
		private void deleteAction() {
			writeInTempDir(config, "delete", null, new FileTreeString(deleteFFT.get()), LOGGER);
			deleter.get()
			.start(fileTree(), deleteFFT.get());
		}
		
		private FileTree fileTree() {
			return meta.getFileTree();
		}

		@Override
		public void stateChange(State s) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void failed(String msg, Throwable error) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startWalking(Path path) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endWalking(Path path) {
			// TODO Auto-generated method stub
			
		}

	}
	
	private FilesView openFilesView(String title, Dir dir, FilesViewSelector selector) {
		// FIXME Auto-generated method stub
		return Junk.notYetImplemented();
	}
		private Node header(String string) {
		return addClass(new Label(string), "text", "header");
	}
	private Text text(String str) {
		return FxText.text(str, "text");
	}
	
	/** FIXME
	 * 	@Override
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
	
		@Override
	public void walkCompleted() {
		FilteredDir backup =  fileTree().filtered(f -> f.getStatus().isBackupable());
		FilteredDir delete = !config.getBackupConfig().hardSync() ? null :  fileTree().filtered(f -> f.getStatus().isBackupDeletable());

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
	 */
	
	public void finish(MetaTabContent v, String msg, boolean failed) {
		removeClass(v, "disable", "completed");
		removeClass(v.bottomText, "disable-text", "completed-text");
		String s = failed ? "disable" : "completed";
		addClass(v, s);
		addClass(v.bottomText, s+"-text");
		v.bottomText.setText(msg);
	}
}
