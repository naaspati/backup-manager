package sam.backup.manager.config.view;

import static sam.backup.manager.extra.Utils.hyperlink;
import static sam.backup.manager.extra.Utils.millsToTimeString;
import static sam.backup.manager.extra.Utils.saveFiletree;
import static sam.backup.manager.extra.Utils.showErrorDialog;
import static sam.backup.manager.extra.Utils.showStage;
import static sam.fx.helpers.FxHelpers.setClass;
import static sam.fx.helpers.FxHelpers.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.console.ansi.ANSI;
import sam.fx.helpers.FxHelpers;
import sam.fx.popup.FxPopupShop;

public class ListingView extends VBox implements ICanceler, IStopStart, ButtonAction {
	private final Config config;
	private final IStartOnComplete<ListingView> startEnd;
	private volatile boolean cancel;
	private String treeText;
	private CustomButton button;
	private Text fileCountT, dirCountT;  
	private final Path listPath;

	public ListingView(Config c,Path listPath, Long lastUpdated, IStartOnComplete<ListingView> startEnd) {
		setClass(this, "listing-view");
		config = c;
		this.listPath = listPath;
		this.startEnd = startEnd;

		button = new CustomButton(ButtonType.WALK, this);

		Hyperlink header = hyperlink(config.getSource());
		FxHelpers.addClass(header, "header");
		fileCountT = text("  Files: --", "count-text");
		dirCountT = text("  Dirs: --", "count-text");

		getChildren().addAll(header, new HBox(10, fileCountT, dirCountT), text("Last updated: "+millsToTimeString(lastUpdated)), button);
	}

	@Override
	public void handle(ButtonType type) {
		switch (type) {
			case WALK:
				if(config.is1DepthWalk())
					start1Depth();
				else
					start();
				break;
			case CANCEL:
				stop();
				break;
			case OPEN:
				TextArea ta = new TextArea(treeText);
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
		System.out.println(ANSI.yellow("1-depth walk: ")+root);

		if(!Files.isDirectory(root)) {
			FxPopupShop.showHidePopup("dir not found: \n"+root, 1500);
			System.out.println(ANSI.red("dir not found: "+root));
			return;
		}
		String[] s = root.toFile().list();
		treeText = Stream.of(s).collect(Collectors.joining("\n |", config.getSource()+"\n |", ""));
		
		Platform.runLater(() ->{
			dirCountT.setText(null);
			fileCountT.setText("All count: "+s.length);
		});
		updateRootFileTree();
	}

	@Override
	public void stop() {
		cancel = true;
	}
	@Override
	public void start() {
		cancel = false;
		startEnd.start(this);
	}
	@Override
	public boolean isCancelled() {
		return cancel;
	}
	public Config getConfig() {
		return config;
	}
	public void updateRootFileTree() {
		if(treeText == null) {
			treeText = config.getFileTree().toTreeString();
			try {
				config.getFileTree().setDirsModified();
				saveFiletree(config);
			} catch (IOException e) {
				showErrorDialog(config.getSource(), "failed to save filetree", e);
			}
		}

		Platform.runLater(() -> {
			getChildren().remove(button);
			button.setType(ButtonType.OPEN);
			getChildren().add(new HBox(3, button, new CustomButton(ButtonType.SAVE, this)));
		});
	}
	public void save() {
		if(treeText == null) {
			showErrorDialog(null, "FileTreeEntity not set", null);
			return;
		}
		Platform.runLater(() -> {
			Path p = config.getTarget() != null ? config.getTarget()  :  (listPath == null ? config.getSource().getParent() : listPath).resolve(config.getSource().getFileName()+"-"+config.getSource().hashCode()+".txt");
			if(Utils.saveToFile(treeText, p)) {
				FxPopupShop.showHidePopup("List created\nfor: "+config.getSource(), 3000);
				startEnd.onComplete(this);
			}
		});
	}
	public void setFileCount(int n) {
		fileCountT.setText("  Files: "+n);
	}
	public void setDirCount(int n) {
		dirCountT.setText("  Dirs: "+n);
	}
}
