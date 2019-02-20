package sam.backup.manager;

import static sam.backup.manager.UtilsFx.fx;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sam.backup.manager.config.api.PathWrap;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxHyperlink;
import sam.fx.helpers.FxUtils;
import sam.myutils.Checker;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;

@Singleton
class UtilsFxImpl implements UtilsFx, Stoppable {
	private static final EnsureSingleton singleton = new EnsureSingleton();

	private final Logger logger = LogManager.getLogger(UtilsFxImpl.class);
	private final ExecutorService POOL;

	public UtilsFxImpl() {
		singleton.init();

		String s = System2.lookup("THREAD_COUNT");
		int size = 1;
		if(s == null) 
			logger.debug("THREAD_COUNT not specified, using single thread pool");
		else {
			try {
				size = Integer.parseInt(s);
				if(size < 1) {
					logger.warn("bad value for THREAD_COUNT={}, using single thread pool", size);
					size = 1;
				}
			} catch (NumberFormatException e) {
				logger.debug("failed to parse THREAD_COUNT=\"{}\"", s, e);
			}
		}

		POOL = size == 1 ? Executors.newSingleThreadScheduledExecutor() : Executors.newFixedThreadPool(size);
	}

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
		if(wrap != null && wrap.path() != null) {
			Hyperlink hyperlink = FxHyperlink.of(wrap.path());
			hyperlink.setMinWidth(400);
			return hyperlink;
		} else {
			Text t = new Text(wrap == null ? "--" : wrap.string());
			t.setDisable(true);
			return t;
		}
	}
	@Override
	public void runAsync(@SuppressWarnings("rawtypes") Task runnable) {
		POOL.execute(runnable);
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
	public void stop() throws Exception {
		POOL.shutdownNow();
		logger.warn("waiting thread to die");
		POOL.awaitTermination(2, TimeUnit.SECONDS);
	}

	@Override
	public Node headerBanner(String text) {
		// FIXME something beutiful 
		return new Text(text);
	}
	@Override
	public Node bigPlaceholder(String text) {
		// FIXME something beutiful 
		return new Text(text);
	}

}
