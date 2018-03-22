
package sam.backup.manager;
import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.showErrorDialog;
import static sam.fx.helpers.FxHelpers.menuitem;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;

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
import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.config.view.RootView;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.TransferView;
import sam.backup.manager.view.ViewType;
import sam.backup.manager.viewers.TransferViewer;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.myutils.fileutils.FilesUtils;

public class App extends Application {
	
	private static Stage stage;
	private static HostServices hs;

	public static Stage getStage() {
		return stage;
	}
	public static HostServices getHostService() {
		return hs;
	}
	private RootConfig root;
	private RootView rootView;
	private StatusView statusView;
	private final BorderPane rootContainer = new BorderPane();

	private ViewSwitcher centerView;
	private List<IStopStart> stoppableTasks ;
	private volatile boolean backupLastPerformedModified;
	private Map<String, Long> backupLastPerformed ;

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
		root = Utils.readConfigJson();

		Path drive = null;
		for (Path p : FileSystems.getDefault().getRootDirectories()) {
			if(Files.exists(p.resolve(".iambackup"))) {
				drive = p;
				break;
			}
		}
		root.init(drive);
		runLater(() -> {
			rootView = new RootView(this.root);
			rootContainer.setTop(new BorderPane(rootView, getMenubar(), null, null, null));
		});

		backupLastPerformed = Utils.readBackupLastPerformedPathTimeMap();  
		stoppableTasks = new ArrayList<>();

		centerView = new ViewSwitcher();
		runLater(() -> rootContainer.setCenter(centerView));

		if(root.hasBackups()) {
			statusView = new StatusView();
			runLater(() -> TransferViewer.getInstance().setStatusView(statusView));
			processConfigs();
		}
		else 
			centerView.setStatus(ViewType.BACKUP, true);

		if(root.hasLists())
			processLists();

		runLater(centerView::firstClick);
	}

	private Node getMenubar() {
		Menu  file = new Menu("_File",
				null,
				menuitem("open app dir", e -> FilesUtils.openFileNoError(Utils.APP_DATA.toFile())),
				menuitem("create FileTree", this::createFileTree)
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
			Utils.saveToFile(ft.toTreeString(), p);
			FxPopupShop.showHidePopup(p.getFileName()+" saved", 1500);
			LogManager.getLogger(getClass()).info("saved: {}", p);
		} catch (IOException e) {
			LogManager.getLogger(getClass()).error("failed to walk: {}", root, e);
			Utils.showErrorDialog(root, "failed to walk", e);
		}
	}

	private void processLists() {
		Path listPath = RootConfig.backupDriveFound() ? root.getFullBackupRoot().resolve("lists") : null ;

		IStartOnComplete<ListingView> action = new IStartOnComplete<ListingView>() {
			@Override
			public void start(ListingView e) {
				Config c = e.getConfig();

				if(c.getDepth() <= 0) {
					showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getDepth(), null);
					return;
				}
				try {
					FileTree f = Utils.readFiletree(c);
					c.setFileTree(f != null ? f : new FileTree(c.getSource()));
				} catch (IOException e1) {
					showErrorDialog(null, "failed to read TreeFile: ", e1);
					return;
				}
				Utils.run(new WalkTask(c, WalkMode.SOURCE, e, e));
			}
			@Override
			public void onComplete(ListingView e) {
				backupLastPerformed.put("list:"+e.getConfig().getSource(), System.currentTimeMillis());
				backupLastPerformedModified = true;
			}
		};

		List<ListingView> list = Stream.of(root.getLists())
				.map(c -> new ListingView(c,listPath,backupLastPerformed.get("list:"+c.getSource()), action))
				.collect(Collectors.toList());

		stoppableTasks.addAll(list);
		runLater(() -> centerView.addAllListView(list));
	}

	private void processConfigs() {
		IStartOnComplete<TransferView> transferAction = new IStartOnComplete<TransferView>() {
			@Override
			public void start(TransferView view) {
				Utils.run(view);
				statusView.addSummery(view.getSummery());
			}
			@Override
			public void onComplete(TransferView view) {
				statusView.removeSummery(view.getSummery());
				backupLastPerformed.put("backup:"+view.getConfigView().getConfig().getSource(), System.currentTimeMillis());
				backupLastPerformedModified = true;
				rootView.refreshSize();
				try {
					Utils.saveFiletree(view.getConfigView().getConfig());
				} catch (IOException e) {
					showErrorDialog(null, "failed to save TreeFile: ", e);	
				}
			}
		};

		IStartOnComplete<ConfigView> configAction = new IStartOnComplete<ConfigView>() {
			@Override
			public void start(ConfigView view) {
				if(!view.getConfig().isDisabled()) {
					Config c = view.getConfig();

					if(c.getDepth() <= 0) {
						runLater(() -> view.finish("Walk failed: \nbad value for depth: "+c.getDepth(), true));
						return;
					}
					if(c.getFileTree() == null) {
						FileTree ft;
						try {
							ft = Utils.readFiletree(c);
						} catch (IOException e) {
							showErrorDialog(null, "failed to read TreeFile: ", e);
							return;
						}
						if(ft == null) {
							ft = new FileTree(c.getSource()); 
							c.setFileTree(ft);	
						} else {
							c.setFileTree(ft);
						} 
					}
					Utils.run(new WalkTask(c, WalkMode.BOTH, view, view));
				}
			}
			@Override
			public void onComplete(ConfigView view) {
				if(view.hashBackups())
					runLater(() -> centerView.add(new TransferView(view, statusView, transferAction)));
				else {
					backupLastPerformed.put("backup:"+view.getConfig().getSource(), System.currentTimeMillis());
					backupLastPerformedModified = true;
				}
			}
		};

		runLater(() -> Stream.of(root.getBackups())
				.map(c -> new ConfigView(c, configAction, backupLastPerformed.get("backup:"+c.getSource())))
				.peek(stoppableTasks::add).forEach(centerView::add)
				);
	}

	@Override
	public void stop() throws Exception {
		if(backupLastPerformedModified)
			Utils.saveBackupLastPerformedPathTimeMap(backupLastPerformed);
		System.exit(0);
	}
}
