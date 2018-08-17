package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.hashedName;
import static sam.backup.manager.extra.Utils.hyperlink;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.backup.manager.extra.Utils.saveFiletree;
import static sam.backup.manager.extra.Utils.saveToFile2;
import static sam.backup.manager.extra.Utils.showErrorDialog;
import static sam.backup.manager.extra.Utils.showStage;
import static sam.backup.manager.extra.VariablesKeys.LIST_BACKUP_DIR;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.App;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.DirEntity;
import sam.backup.manager.file.FileEntity;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.console.ANSI;
import sam.fx.helpers.FxText;
import sam.fx.popup.FxPopupShop;
import sam.string.StringUtils;

public class ListingView extends VBox implements ICanceler, IStopStart, ButtonAction, WalkListener {
	private static final Logger LOGGER =  LoggerFactory.getLogger(ListingView.class);

	public static boolean saveWithoutAsking;

	private final Config config;
	private final IStartOnComplete<ListingView> startEnd;
	private volatile boolean cancel;
	private CharSequence treeText;
	private CustomButton button;
	private Text fileCountT, dirCountT;  
	private Consumer<ListingView> onWalkCompleted;

	public ListingView(Config c, Long lastUpdated, IStartOnComplete<ListingView> startEnd) {
		setClass(this, "listing-view");
		config = c;
		this.startEnd = startEnd;

		Path src = config.getSource();
		if(src == null || Files.notExists(src)) {
			Hyperlink h = new Hyperlink(String.valueOf(src));
			addClass(h, "header");
			getChildren().addAll(h, FxText.ofString("Last updated: "+millsToTimeString(lastUpdated)));
			setDisable(true);
		} else {
			button = new CustomButton(ButtonType.WALK, this);

			Hyperlink header = hyperlink(src);
			addClass(header, "header");
			fileCountT = FxText.text("  Files: --", "count-text");
			dirCountT = FxText.text("  Dirs: --", "count-text");

			getChildren().addAll(header, new HBox(10, fileCountT, dirCountT), FxText.ofString("Last updated: "+millsToTimeString(lastUpdated)), button);
		}


	}

	@Override
	public void handle(ButtonType type) {
		switch (type) {
			case WALK:
				start();
				break;
			case CANCEL:
				stop();
				break;
			case OPEN:
				TextArea ta = new TextArea(treeText.toString());
				ta.setEditable(false);
				showStage(ta);
				break;
			case SAVE:
				save();
				break;
			default:
				break;
		}
	}

	private void start1Depth() {
		final Path root = config.getSource();
		LOGGER.info(ANSI.yellow("1-depth walk: ")+root);

		if(!Files.isDirectory(root)) {
			FxPopupShop.showHidePopup("dir not found: \n"+root, 1500);
			LOGGER.info(ANSI.red("dir not found: "+root));
			return;
		}
		String[] names = root.toFile().list();
		StringBuilder treeText = new StringBuilder()
				.append(config.getSource())
				.append("\n |");

		for (int i = 0; i < names.length - 1; i++)
			treeText.append(names[i]).append("\n |");

		treeText.append(names[names.length - 1]).append('\n');

		this.treeText = treeText;

		runLater(() ->{
			dirCountT.setText(null);
			fileCountT.setText("All count: "+names.length);
		});
		walkCompleted();
	}

	@Override
	public void stop() {
		cancel = true;
	}
	@Override
	public void start() {
		cancel = false;
		if(config.getBackupConfig().getDepth() == 1)
			start1Depth();
		else
			startEnd.start(this);
	}
	@Override
	public boolean isCancelled() {
		return cancel;
	}
	public Config getConfig() {
		return config;
	}
	@Override
	public void walkFailed(String reason, Throwable e) {
		LOGGER.info(ANSI.red(reason));
		e.printStackTrace();
	}
	private volatile int fileCount, dirCount;

	@Override
	public void onFileFound(FileEntity ft, long size, WalkMode mode) {
		runLater(() -> fileCountT.setText("  Files: "+(++fileCount)));
	}
	@Override
	public void onDirFound(DirEntity ft, WalkMode mode) {
		runLater(() -> dirCountT.setText("  Dirs: "+(++dirCount)));
	}
	@Override
	public void walkCompleted() {
		if(treeText == null) {
			treeText = new FileTreeString(config.getFileTree());
			try {
				saveFiletree(config, TreeType.BACKUP);
			} catch (IOException e) {
				showErrorDialog(config.getSource(), "failed to save filetree", e);
			}
		}

		runLater(() -> {
			getChildren().remove(button);
			button.setType(ButtonType.OPEN);
			getChildren().add(new HBox(3, button, new CustomButton(ButtonType.SAVE, this)));
			if(onWalkCompleted != null)
				onWalkCompleted.accept(this);
		});
	}
	public void setOnWalkCompleted(Consumer<ListingView> onWalkCompleted) {
		this.onWalkCompleted = onWalkCompleted;
	}
	public void save() {
		if(treeText == null) {
			showErrorDialog(null, "FileTreeEntity not set", null);
			return;
		}
		Path p = config.getTargetRaw() != null ? config.getTarget() : null;
		Path name = p == null ? Paths.get(hashedName(config.getSource(), ".txt")) : p.getFileName();
		
		if(saveWithoutAsking) {
			String listbackDirs = App.getRootConfig().getVariable(LIST_BACKUP_DIR);
			
			if(listbackDirs == null)
				LOGGER.warn("no property specified for: "+LIST_BACKUP_DIR+", thus no list saving performed in defaults dirs");
			else {
				for(String str: StringUtils.split(listbackDirs, ';')) 
					write(Paths.get(str).resolve(name));
			}
			if(p == null) {
				if(saveToFile2(treeText, p))
					listCreated();
			} else {
				write(p);
				listCreated();
			}
		} else if(saveToFile2(treeText, p)) 
			listCreated();

	}
	private void write(Path p) {
		if(p == null)
			return;
		
		try {
			Files.createDirectories(p.getParent());
			Utils.write(p, treeText);
			LOGGER.info("files-tree created: {}", p);
		} catch (IOException e) {
			showErrorDialog(p, "failed to save tree", e);
		}
	}

	private void listCreated() {
		runLater(() -> {
			FxPopupShop.showHidePopup("List created\nfor: "+config.getSource(), 3000);
			startEnd.onComplete(this);
		});
	}
}
