package sam.backup.manager.view.config;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.hashedName;
import static sam.backup.manager.extra.Utils.hyperlink;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.backup.manager.extra.Utils.showErrorDialog;
import static sam.backup.manager.extra.Utils.showStage;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.config.PathWrap;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.Dir;
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
import sam.myutils.Checker;
import sam.myutils.System2;
import sam.string.StringUtils;

public class ListingView extends VBox implements ButtonAction, WalkListener {
	private static final Logger LOGGER =  Utils.getLogger(ListingView.class);

	public static boolean saveWithoutAsking;

	private final Config config;
	private volatile boolean cancel;
	private CharSequence treeText;
	private CustomButton button;
	private Text fileCountT, dirCountT;  
	private Consumer<ListingView> onWalkCompleted;

	public ListingView(Config c, Long lastUpdated) {
		setClass(this, "listing-view");
		config = c;
		
		Node src = hyperlink(config.getSource());
		if(src instanceof VBox)
			((VBox) src).getChildren().forEach(h -> addClass(h, "header"));
		else
			addClass(src, "header");

		if(Checker.isEmpty(config.getSource())) {
			getChildren().addAll(src, FxText.ofString("Last updated: "+millsToTimeString(lastUpdated)));
			setDisable(true);
		} else {
			button = new CustomButton(ButtonType.WALK, this);
			fileCountT = FxText.text("  Files: --", "count-text");
			dirCountT = FxText.text("  Dirs: --", "count-text");

			getChildren().addAll(src, new HBox(10, fileCountT, dirCountT), FxText.ofString("Last updated: "+millsToTimeString(lastUpdated)), button);
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
				TextArea ta = new TextArea(treeText == null ? null : treeText.toString());
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
		StringBuilder treeText = new StringBuilder();
		
		for (PathWrap pw : config.getSource()) {
			if(pw.path() == null) {
				LOGGER.info("unresolved: "+pw);
				continue;
			}

			Path root = pw.path(); 
			LOGGER.info("1-depth walk: "+root);

			if(!Files.isDirectory(root)) {
				FxPopupShop.showHidePopup("dir not found: \n"+root, 1500);
				LOGGER.info("dir not found: "+root);
				return;
			}
			String[] names = root.toFile().list();
					treeText
					.append(config.getSource())
					.append("\n |");

			for (int i = 0; i < names.length - 1; i++)
				treeText.append(names[i]).append("\n |");

			treeText.append(names[names.length - 1]).append('\n');

			runLater(() ->{
				dirCountT.setText(null);
				fileCountT.setText("All count: "+names.length);
			});
			
			treeText.append('\n');
		}

		this.treeText = treeText;
		walkCompleted();
	}

	public void stop() {
		cancel = true;
	}
	public void start() {
		cancel = false;
		if(config.getWalkConfig().getDepth() == 1)
			start1Depth();
		else
			startEnd.start(this);
	}
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
	public void onDirFound(Dir ft, WalkMode mode) {
		runLater(() -> dirCountT.setText("  Dirs: "+(++dirCount)));
	}
	@Override
	public void walkCompleted() {
		if(treeText == null) {
			treeText = new FileTreeString(config.getFileTree());
			Utils.saveFileTree(config);
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
		if(Checker.isEmpty(treeText)) {
			showErrorDialog(null, "FileEntity not set", null);
			return;
		}
		boolean created[] = {false};

		Path p = Optional.ofNullable(config.getBaseTarget()).map(PathWrap::path).orElse(null);
		Path name = p == null ? Paths.get(hashedName(config.getBaseTarget().path(), ".txt")) : p.getFileName();

		if(saveWithoutAsking) {
			String listbackDirs = System2.lookup("LIST_BACKUP_DIR");

			if(listbackDirs == null)
				LOGGER.warn("no var specified for: LIST_BACKUP_DIR, thus no list saving performed in defaults dirs");
			else {
				for(String str: StringUtils.split(listbackDirs, ';')) 
					write(Paths.get(str).resolve(name), treeText);
			}

			if(p == null) {
				if(saveToFile2(treeText, p)) 
					created[0] = true; 
			} else {
				write(p, treeText);
				listCreated();
			}
		} else {
			saveToFile2(treeText, p);
		} 
		if(created[0])
			listCreated();
	}
	private boolean saveToFile2(CharSequence text, Path p) {
		return Utils.saveToFile2(p.getParent().toFile(), p.getFileName().toString(), "Save File Tree", text);
	}
	private void write(Path p, CharSequence data) {
		if(p == null)
			return;

		try {
			Files.createDirectories(p.getParent());
			Utils.write(p, data);
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
