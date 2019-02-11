
package sam.backup.manager.view;
import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxMenu.menuitem;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerFactory;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.view.config.AboutDriveView;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.Checker;

public class App extends Application {
	private static ConfigManager configManager;
	private AboutDriveView aboutDriveView;
	private StatusView statusView;
	private final BorderPane rootContainer = new BorderPane();
	private final AtomicBoolean stopping = new AtomicBoolean(false);

	private ViewSwitcher centerView;
	private Stage stage;

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);
		FileOpenerNE.setErrorHandler((file, e) -> FxAlert.showErrorDialog(file, "Failed to open File", e));
		Utils.setErrorHandler((file, e) -> runLater(() -> FxAlert.showErrorDialog(file, "Failed to open File", e)));

		Utils.set(stage, getHostServices());
		ProgressIndicator pi = new ProgressIndicator();
		pi.setMaxWidth(40);
		rootContainer.setCenter(pi);

		Scene scene = new Scene(rootContainer);
		scene.getStylesheets().add("styles.css");

		stage.setScene(scene);
		stage.setWidth(500);
		stage.setHeight(500);
		stage.show();

		Thread t = new Thread(this::secondStart);
		t.setDaemon(true);
		t.start();
	}
	private void secondStart() {
		configManager = ConfigManagerFactory.defaultInstance();
		
		runLater(() -> {
			aboutDriveView = new AboutDriveView(configManager);
			rootContainer.setTop(new BorderPane(aboutDriveView, getMenubar(), null, null, null));
		});

		centerView = new ViewSwitcher();
		runLater(() -> rootContainer.setCenter(centerView));

		if(Checker.isNotEmpty(configManager.getBackups())) {
			statusView = new StatusView();
			runLater(() -> TransferViewer.getInstance().setStatusView(statusView));
			ConfigViews.init(statusView, aboutDriveView, centerView, configManager);
		} else 
			centerView.setStatus(ViewType.BACKUP, true);

		if(Checker.isNotEmpty(configManager.getLists()))
			 ListsViews.init(configManager, centerView);

		runLater(centerView::firstClick);
	}

	private Node getMenubar() {
		Menu  file = new Menu("_File",
				null,
				menuitem("open app dir", e -> FileOpenerNE.openFile(new File(".")))
				//TODO menuitem("cleanup", e -> new Cleanup())
				);
		return new MenuBar(file);
	}
	
	@Override
	public void stop() throws Exception {
		if(!stopping.compareAndSet(false, true))
			return;
		
		Utils.stop();
		System.exit(0); 
	}
	public static ConfigManager getConfigManager() {
		return configManager;
	}
}
