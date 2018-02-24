
package sam.backup.manager;

import static javafx.application.Platform.runLater;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.config.view.RootView;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.view.CenterView;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.TransferView;
import sam.backup.manager.view.ViewType;
import sam.backup.manager.walk.WalkTask;
import sam.backup.manager.walk.WalkType;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.myutils.fileutils.FilesUtils;

// FIXME worst choice object serializing FileTree 
public class Main extends Application {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if(args.length == 1 && args[0].equals("open")) {
			FilesUtils.openFileNoError(new File("."));
			System.exit(0);
		}
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			System.out.println("thread: "+thread.getName());
			exception.printStackTrace();
		});
		launch(args);
	}
	
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

	private CenterView centerView;
	private List<IStopStart> stoppableTasks ;
	private volatile boolean backupLastPerformedModified;
	private Map<String, Long> backupLastPerformed ;

	@Override
	public void start(Stage stage) throws Exception {
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);
		
		Main.stage = stage;
		Main.hs = getHostServices();

		ProgressIndicator pi = new ProgressIndicator();
		pi.setMaxWidth(40);
		rootContainer.setCenter(pi);

		Scene scene = new Scene(rootContainer);
		scene.getStylesheets().add("style.css");

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
			rootContainer.setTop(rootView);
		});
		
		backupLastPerformed = Utils.readBackupLastPerformedPathTimeMap();  
		stoppableTasks = new ArrayList<>();
		
		centerView = new CenterView();
		runLater(() -> rootContainer.setCenter(centerView));

		if(root.hasBackups()) {
			statusView = new StatusView();
			runLater(() -> rootContainer.setBottom(statusView));
			processConfigs();
		}
		else {
			centerView.disable(ViewType.BACKUP);
		}

		if(root.hasLists())
			processLists();
		
		runLater(centerView::firstClick);
	}

	private void processLists() {
		Path listPath = root.isNoDriveMode() ? null : root.getFullBackupRoot().resolve("lists");
		
		ExecutorService ex = Executors.newSingleThreadExecutor();

		IStartOnComplete<ListingView> action = new IStartOnComplete<ListingView>() {
			@Override
			public void start(ListingView e) {
				Config c = e.getConfig();
				c.setFileTree(new FileTree(c.getSource()));
				
				if(c.getDepth() <= 0) {
					runLater(() -> FxAlert.showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getDepth(), null, true));
					return;
				}
				ex.execute(new WalkTask(e));
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
		ExecutorService ex = Executors.newSingleThreadExecutor();

		IStartOnComplete<TransferView> transferAction = new IStartOnComplete<TransferView>() {
			@Override
			public void start(TransferView view) {
				ex.execute(view);
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
					runLater(() -> FxAlert.showErrorDialog(null, "failed to save TreeFile: ", e));	
				}
			}
		};

		IStartOnComplete<ConfigView> configAction = new IStartOnComplete<ConfigView>() {
			@Override
			public void start(ConfigView view) {
				if(!view.getConfig().isDisabled()) {
					view.setLoading(true);
					try {
						Config c = view.getConfig();
						
						if(c.getDepth() <= 0) {
							runLater(() -> view.finish("Walk failed: \nbad value for depth: "+c.getDepth(), true));
							return;
						}
						WalkType w;
						if(c.getFileTree() == null) {
							FileTree ft;
							try {
								ft = Utils.readFiletree(c);
							} catch (ClassNotFoundException | IOException e) {
								runLater(() -> FxAlert.showErrorDialog(null, "failed to read TreeFile: ", e));
								return;
							}
							if(ft == null) {
								w = WalkType.NEW_SOURCE;
								ft = new FileTree(c.getSource()); 
								c.setFileTree(ft);	
							}
							else {
								w = WalkType.SOURCE;
								c.setFileTree(ft);
							} 
						} else {
							w = WalkType.SOURCE;
						}
						
						ex.execute(new WalkTask(view, w));
					} finally {
						
					}
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
