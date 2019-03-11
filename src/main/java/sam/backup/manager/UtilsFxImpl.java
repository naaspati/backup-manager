package sam.backup.manager;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import com.sun.javafx.tk.Toolkit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sam.backup.manager.config.impl.PathWrap;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxText;
import sam.fx.helpers.FxUtils;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.Checker;

@Singleton
@SuppressWarnings("restriction")
class UtilsFxImpl implements IUtilsFx {
	private final Logger logger = Utils.getLogger(UtilsFxImpl.class);

	@Override
	public Stage showStage(Window parent, Parent content) {
		Stage stg = new Stage();
		stg.initModality(Modality.WINDOW_MODAL);
		stg.initStyle(StageStyle.UTILITY);
		stg.initOwner(parent);
		stg.setScene(new Scene(content));
		stg.getScene().getStylesheets().setAll(parent.getScene().getStylesheets());
		stg.setWidth(300);
		stg.setHeight(400);
		fx(stg::show);

		return stg;
	}

	@Override
	public void showErrorDialog(Object text, String header, Exception error) {
		fx(() -> FxAlert.showErrorDialog(text, header, error));
	}
	@Override
	public FileChooser fileChooser(File expectedDir, String expectedName, String title) {
		return FxUtils.fileChooser(expectedDir, expectedName, title, null);
	}
	@Override
	public void stylesheet(Parent node) {
		String name = "stylesheet/" + node.getClass().getSimpleName() + ".css";
		URL url = ClassLoader.getSystemResource(name);
		if (url == null) {
			logger.error("stylesheet not found: " + name);
			return;
		}
		node.getStylesheets().add(url.toExternalForm());
	}

	@Override
	public Node hyperlink(PathWrap wrap) {
		if(wrap == null || wrap.string() == null)
			return disabledText("--");
		
		Hyperlink link = new Hyperlink(wrap.string());
		link.setOnAction(e -> {
			Path  p = wrap.path();
			if(Files.notExists(p))
				FxAlert.showErrorDialog(p, "File/dir not found", p);
			else 
				FileOpenerNE.openFileLocationInExplorer(p.toFile());
			link.setVisited(false);
		});
		link.setWrapText(true);
		return link;
	}
	
	
	private Node disabledText(String s) {
		Text t = new Text(s);
		t.setDisable(true);
		
		return t;
	}

	@Override
	public Node hyperlink(List<PathWrap> wraps) {
		if(Checker.isEmpty(wraps))
			return hyperlink((PathWrap)null);
		if(wraps.size() == 1)
			return hyperlink(wraps.get(0));
		VBox box = new VBox(2);
		wraps.forEach(p -> box.getChildren().add(hyperlink(p)));

		return box;
	}
	@Override
	public Node button(String tooltip, String icon, EventHandler<ActionEvent> action) {
		Button button = new Button();
		button.getStyleClass().clear();
		button.setTooltip(new Tooltip(tooltip));
		button.setGraphic(new ImageView(icon));
		button.setOnAction(action);
		return button;
	}

	@Override
	public Node headerBanner(String text) {
		Label label = new Label(text);
		label.getStyleClass().add("header-banner");
		return label;
	}
	@Override
	public Node bigPlaceholder(String text) {
		return FxText.text(text, "big-placeholder");
	}
	@Override
	public void fx(Runnable runnable) {
		Platform.runLater(runnable);
	}
	
	private final Toolkit kit = Toolkit.getToolkit();
	
	
	@Override
	public void ensureFxThread() {
		kit.checkFxUserThread();
	}
	@Override
	public Window window(Injector injector) {
		return injector.instance(Window.class, ParentWindow.class);
	}

}
