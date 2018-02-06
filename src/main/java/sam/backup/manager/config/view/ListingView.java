package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxHelpers.setClass;
import static sam.fx.helpers.FxHelpers.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.view.enums.ButtonType;
import sam.fx.alert.FxAlert;

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

		Text header = text(String.valueOf(config.getSource()), "header");
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
			getChildren().add(new HBox(3, button, listPath == null ? new Text() : new CustomButton(ButtonType.SAVE, this)));
		});
	}
	public void save() {
		if(treeText == null) {
			runLater(() -> FxAlert.showErrorDialog(null, "FileTree not set", null));
			return;
		}
		Path p = listPath.resolve(config.getSource().getFileName()+"-"+config.getSource().hashCode()+".txt");

		try {
			Files.write(p, treeText.toString().getBytes(StandardCharsets.UTF_16), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			runLater(() -> FxAlert.showMessageDialog("at: "+p+"for: "+config.getSource(), "List create"));
			startEnd.onComplete(this);
		} catch (IOException e) {
			runLater(() -> FxAlert.showErrorDialog("target: "+p, "failed to save:" , e));
		}
	}

	public void setFileCount(int n) {
		fileCountT.setText("  Files: "+n);
	}
	public void setDirCount(int n) {
		dirCountT.setText("  Dirs: "+n);
	}

}
