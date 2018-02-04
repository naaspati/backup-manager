package sam.backup.manager.view;

import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxHelpers.setClass;
import static sam.fx.helpers.FxHelpers.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.config.Config;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;
import sam.fx.alert.FxAlert;

public class ListingView extends VBox implements ICanceler, IStopStart, Consumer<ButtonType> {
	private final Config config;
	private final Consumer<ListingView> startAction, whenSaved;
	private volatile boolean cancel;
	private FileTree tree;
	private String treeText;
	private CustomButton button;
	private Text fileCountT, dirCountT;  
	private final Path listPath;

	public ListingView(Config c,Path listPath, Long lastUpdated, Consumer<ListingView> start, Consumer<ListingView> whenSaved) {
		setClass(this, "listing-view");
		config = c;
		startAction = start;
		this.listPath = listPath;
		this.whenSaved = whenSaved;

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
			Stage stg = new Stage();
			stg.initModality(Modality.APPLICATION_MODAL);
			stg.initStyle(StageStyle.UTILITY);
			stg.setScene(new Scene(new TextArea(treeText)));
			runLater(stg::show);
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
		startAction.accept(this);
	}
	@Override
	public boolean isCancelled() {
		return cancel;
	}
	public Config getConfig() {
		return config;
	}
	public void setFileTree(FileTree tree) {
		Objects.requireNonNull(tree);

		this.tree = tree;

		StringBuilder sb = new StringBuilder();

		sb.append(tree.getPath()).append('\n');
		tree.append(sb);
		treeText = sb.toString();
		
		runLater(() -> {
			getChildren().remove(button);
			button.setType(ButtonType.OPEN);
			getChildren().add(new HBox(3, button, new CustomButton(ButtonType.SAVE, this)));
		});
	}
	public void save() {
		if(treeText == null) {
			runLater(() -> FxAlert.showErrorDialog(null, "FileTree not set", null));
			return;
		}
		Path p = listPath.resolve(tree.getPath().getFileName()+"-"+tree.getPath().hashCode()+".txt");

		try {
			Files.write(p, treeText.toString().getBytes(StandardCharsets.UTF_16), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			runLater(() -> FxAlert.showMessageDialog("at: "+p+"for: "+tree.getPath(), "List create"));
			whenSaved.accept(this);
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
