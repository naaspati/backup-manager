package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.fx.alert.FxAlert.showErrorDialog;
import static sam.fx.helpers.FxHelpers.setClass;
import static sam.fx.helpers.FxHelpers.text;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import sam.backup.manager.Main;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.fx.helpers.FxHelpers;
import sam.fx.popup.FxPopupShop;

public class ListingView extends VBox implements ICanceler, IStopStart, Consumer<ButtonType> {
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

		Hyperlink header = Utils.hyperlink(config.getSource());
		FxHelpers.addClass(header, "header");
		fileCountT = text("  Files: --", "count-text");
		dirCountT = text("  Dirs: --", "count-text");

		getChildren().addAll(header, new HBox(10, fileCountT, dirCountT), text("Last updated: "+Utils.millsToTimeString(lastUpdated)), button);
	}

	@Override
	public void accept(ButtonType type) {
		switch (type) {
		case WALK:
			start();
			break;
		case CANCEL:
			stop();
			break;
		case OPEN:
			TextArea ta = new TextArea(treeText);
			ta.setEditable(false);
			Utils.showStage(ta);
			break;
		case SAVE:
			save();
			break;
		default:
			break;
		}
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
	public void updateFileTree() {
		treeText = config.getFileTree().toTreeString();
		
		runLater(() -> {
			getChildren().remove(button);
			button.setType(ButtonType.OPEN);
			getChildren().add(new HBox(3, button, new CustomButton(ButtonType.SAVE, this)));
		});
	}
	public void save() {
		if(treeText == null) {
			runLater(() -> showErrorDialog(null, "FileTree not set", null));
			return;
		}
		runLater(() -> {
			FileChooser fc = new FileChooser();

			Path p = config.getTarget() != null ? config.getTarget()  :  (listPath == null ? config.getSource().getParent() : listPath).resolve(config.getSource().getFileName()+"-"+config.getSource().hashCode()+".txt");
			
			fc.setInitialDirectory(p.getParent().toFile());
			fc.setInitialFileName(p.getFileName().toString());
			fc.setTitle("save filetree");
			File file = fc.showSaveDialog(Main.getStage());
			
			if(file == null)
				return;
			
			try {
				Files.write(file.toPath(), treeText.toString().getBytes(StandardCharsets.UTF_16), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				FxPopupShop.showHidePopup("List created\nat: "+file+"\nfor: "+config.getSource(), 3000);
				startEnd.onComplete(this);
			} catch (IOException e) {
				showErrorDialog("target: "+file, "failed to save" , e);
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
