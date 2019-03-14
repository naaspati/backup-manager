package sam.backup.manager.view.backup;

import static sam.backup.manager.Utils.bytesToString;
import static sam.backup.manager.Utils.millsToTimeString;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Provider;

import org.apache.logging.log4j.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.PathWrap;
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
import sam.backup.manager.view.ViewBase;
import sam.backup.manager.view.ViewsBase;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.fx.helpers.FxConstants;
import sam.fx.helpers.FxGridPane;
import sam.fx.helpers.FxLabel;
import sam.fx.helpers.FxText;
import sam.nopkg.Junk;

class BackupView extends ViewBase {
	private final Logger LOGGER = Utils.getLogger(BackupView.class);

	private final SimpleObjectProperty<FileTree> currentFileTree = new SimpleObjectProperty<>();
	private final Provider<Deleter> deleter;
	private final boolean SAVE_EXCLUDE_LIST;
	private Path _tempDir;
	
	public BackupView(Config config, FileTreeManager factory, Executor executor, Provider<Deleter> deleter, boolean saveExcludedList) {
		super(config, "config-view", factory, executor);
		this.deleter = deleter;
		this.SAVE_EXCLUDE_LIST = saveExcludedList;
		
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
	private Path tempDir() {
		if(_tempDir == null)
			_tempDir = Utils.tempDirFor(config);
		
		return _tempDir;
	}
	
	@Override
	protected Node createRoot(List<FileTreeMeta> metas) {
		if(metas.size() == 1)
			return new MetaTabContent(metas.get(0));
		else 
			return new TabPane(metas.stream().map(MetaTab::new).toArray(Tab[]::new));
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
	
	private class MetaTabContent extends BorderPane implements ButtonAction, WalkListener  {
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

		public MetaTabContent(FileTreeMeta meta) {
			setPadding(FxConstants.INSETS_10);
			this.meta = meta;
			
			addClass(this, "meta-content");
			setContextMenu();
			
			PathWrap source = meta.getSource();
			PathWrap target = meta.getTarget();
			long lastUpdated = meta.getLastModified();
			
			VBox top = new VBox(5,
					hbox("Source: ",source),
					hbox("Target: ",target) 
					);
			
			setTop(top);

			Label summeyLabel = FxLabel.label("SUMMERY", "summery");
			summeyLabel.setMaxWidth(Double.MAX_VALUE);
			summeyLabel.setAlignment(Pos.CENTER);
			
			sourceSizeT = text("---");
			sourceFileCountT = text("---");
			sourceDirCountT = text("---"); 

			String st = config.getWalkConfig().walkBackup() ? "--" : "N/A";
			targetSizeT = text(st); 
			targetFileCountT = text(st); 
			targetDirCountT = text(st);

			backupSizeT = text("---");
			backupFileCountT = text("---");
			
			GridPane tiles = FxGridPane.gridPane(15, 5);
			int row = 0;
			tiles.addRow(row++, text("Last updated: "), colSpan(text(lastUpdated <= 0 ? "N/A" : millsToTimeString(lastUpdated)), GridPane.REMAINING));
			tiles.add(summeyLabel, 0, row++, GridPane.REMAINING, 1);
			tiles.addRow(row++, colHeaderText(""), header("Source"), header("Backup"), header("New/Modified"));
			tiles.addRow(row++, colHeaderText(" size |"), sourceSizeT, targetSizeT, backupSizeT);
			tiles.addRow(row++, colHeaderText("files |"), sourceFileCountT, targetFileCountT, backupFileCountT);
			tiles.addRow(row++, colHeaderText(" dirs |"), sourceDirCountT, targetDirCountT);
			
			setCenter(tiles);
			bottomText = new Text();

			if(!ViewsBase.exists(meta)) {
				setBottom(bottomText);
				finish("Source not found", true);
			} else {
				HBox buttons = new HBox(5, walk, files, delete, bottomText);
				buttons.setDisable(true); //TODO remove, when app is working
				setBottom(buttons);
				files.setVisible(false);
				delete.setVisible(false);
			}
			
			BorderPane.setMargin(getBottom(), new Insets(15, 5, 0, 5));
			
		}
		
		private Node colHeaderText(String string) {
			Text text = new Text(string);
			GridPane.setHalignment(text, HPos.RIGHT);
			return text;
		}

		private Node hbox(String title, PathWrap p) {
			Text text = new Text(title);
			Node link = hyperlink(p);
			
			HBox hbox = new HBox(5, text, link);
			hbox.setAlignment(Pos.CENTER_LEFT);
			HBox.setHgrow(link, Priority.ALWAYS);
			
			return hbox;
		}

		public void finish(String msg, boolean failed) {
			removeClass(this, "disable", "completed");
			removeClass(bottomText, "disable-text", "completed-text");
			String s = failed ? "disable" : "completed";
			addClass(this, s);
			addClass(bottomText, s+"-text");
			bottomText.setText(msg);
		}
		
		private Node colSpan(Node node, int colSpan) {
			GridPane.setColumnSpan(node, colSpan); 
			return node;
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
				fx(() -> finish("Walk failed: \nbad value for depth: "+config.getWalkConfig().getDepth(), true));
				return false;
			}
			if(fileTree() == null) {
				try {
					if(meta.loadFiletree(manager, false) == null)
						meta.loadFiletree(manager, true);
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
					sourceFileCountT.setText(Utils.toString(++sourceFileCount));
				} else if(mode == WalkMode.BACKUP){
					targetSizeT.setText(bytesToString(targetSize += size));
					targetFileCountT.setText(Utils.toString(++targetFileCount));
				} else {
					throw new IllegalStateException("invalid walkMode: "+mode);
				}
			});
		}
		@Override
		public void onDirFound(Dir ft, WalkMode mode) {
			fx(() -> {
				if(mode == WalkMode.SOURCE) 
					sourceDirCountT.setText(Utils.toString(++sourceDirCount));
				else if(mode == WalkMode.BACKUP)
					targetDirCountT.setText(Utils.toString(++targetDirCount));
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
				backupFileCountT.setText(Utils.toString((int)l[0]));
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
			Utils.writeHandled(tempDir().resolve("delete.txt"), true, w -> {
				w.append(LocalDateTime.now().toString()).append("\n\n");
				manager.writeFileTreeAsString(deleteFFT.get(), w);
				w.append("\n-------------------------------------------\n");
			});
			
			deleter.get().start(fileTree(), deleteFFT.get());
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
	
	/* FIXME
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
		
		// task is WalkTask which is completed
		List<Path> exucludePaths = task.getExucludePaths(); 
		
		if(!exucludePaths.isEmpty() && saveExcludeList)
		  Utils.saveInTempDirHideError(new PathListToFileTree(exucludePaths), config, "excluded", src.getFileName()+".txt");

	}
	 */
}
