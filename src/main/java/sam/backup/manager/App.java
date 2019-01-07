
package sam.backup.manager;
import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxMenu.menuitem;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import sam.backup.manager.config.ConfigReader;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.AboutDriveView;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.ViewType;
import sam.backup.manager.viewers.TransferViewer;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;

public class App extends Application {
	private static Stage stage;
	private static HostServices hs;

	public static Stage getStage() {
		return stage;
	}
	public static HostServices getHostService() {
		return hs;
	}
	private static RootConfig rootConfig;
	private AboutDriveView aboutDriveView;
	private StatusView statusView;
	private final BorderPane rootContainer = new BorderPane();

	private ViewSwitcher centerView;
	private final Set<IStopStart> stoppableTasks = new HashSet<>();

	@Override
	public void start(Stage stage) throws Exception {
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);
		FileOpenerNE.setErrorHandler((file, e) -> FxAlert.showErrorDialog(file, "Failed to open File", e));

		App.stage = stage;
		App.hs = getHostServices();

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
		rootConfig = new ConfigReader().read(Utils.APP_DATA.resolve("config.json"));
		
		runLater(() -> {
			aboutDriveView = new AboutDriveView(rootConfig);
			rootContainer.setTop(new BorderPane(aboutDriveView, getMenubar(), null, null, null));
		});

		centerView = new ViewSwitcher();
		runLater(() -> rootContainer.setCenter(centerView));

		if(rootConfig.hasBackups()) {
			statusView = new StatusView();
			runLater(() -> TransferViewer.getInstance().setStatusView(statusView));
			ConfigManager.init(statusView, aboutDriveView, centerView, rootConfig, stoppableTasks);
		}
		else 
			centerView.setStatus(ViewType.BACKUP, true);

		if(rootConfig.hasLists())
			 ListsManager.init(rootConfig, stoppableTasks, centerView);

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
		stoppableTasks.forEach(IStopStart::stop);
		Utils.stop();
		stage.hide();
	}
	public static RootConfig getRootConfig() {
		return rootConfig;
	}
}
