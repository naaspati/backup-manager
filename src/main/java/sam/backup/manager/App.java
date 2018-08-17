
package sam.backup.manager;
import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxMenu.menuitem;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import sam.backup.manager.cleanup.Cleanup;
import sam.backup.manager.config.ConfigReader;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.AboutDriveView;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.ViewType;
import sam.backup.manager.viewers.TransferViewer;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.backup.manager.walk.WalkMode;
import sam.fileutils.FileOpener;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;

public class App extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
	
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
		Path path;
		try {
			path = Files.list(Utils.APP_DATA_DIR.resolve("configs")).filter(p -> p.getFileName().toString().endsWith(".json")).findFirst().orElse(null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		rootConfig = new ConfigReader().read(path);
		
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
				menuitem("open app dir", e -> FileOpener.getInstance().openFileNoError(Utils.APP_DATA_DIR.toFile())),
				menuitem("create FileTree", this::createFileTree),
				menuitem("cleanup", e -> new Cleanup())
				);
		return new MenuBar(file);
	}

	private void createFileTree(Object ignore) {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Select dir to make FileTree");
		File file = dc.showDialog(stage);

		if(file == null)
			return;

		Path root = file.toPath();
		FileTree ft = new FileTree(root);

		try {
			ft.walkStarted(root);
			Files.walkFileTree(root, new SimpleFileVisitor<Path> () {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					ft.addFile(file, new Attrs(attrs.lastModifiedTime().toMillis(), attrs.size()), WalkMode.SOURCE);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if(!ft.isRootPath(dir))
						ft.addDirectory(dir, new Attrs(attrs.lastModifiedTime().toMillis(), 0), WalkMode.SOURCE);
					return FileVisitResult.CONTINUE;
				}
			});
			
			ft.walkCompleted();
			Path p = root.resolveSibling(root.getFileName()+".txt");
			Utils.saveToFile2(new FileTreeString(ft), p);
			FxPopupShop.showHidePopup(p.getFileName()+" saved", 1500);
			LOGGER.info("saved: {}", p);
		} catch (IOException e) {
			LOGGER.error("failed to walk: {}", root, e);
			Utils.showErrorDialog(root, "failed to walk", e);
		}
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
