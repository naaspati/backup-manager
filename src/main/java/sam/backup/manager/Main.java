
package sam.backup.manager;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
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
import sam.backup.manager.file.AboutFile;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.view.CenterView;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.TransferView;
import sam.backup.manager.view.enums.ViewType;
import sam.backup.manager.walk.WalkTask;
import sam.backup.manager.walk.WalkType;
import sam.fx.alert.FxAlert;

public class Main extends Application {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			System.out.println("thread: "+thread.getName());
			exception.printStackTrace();
		});
		launch(args);
	}
	
	private static Stage stage;
	private static Set<Config> forceModified; 
	
	public static Stage getStage() {
		return stage;
	}
	public static void addToBeModified(Config c) {
		if(forceModified == null) forceModified = new HashSet<>();
		forceModified.add(c);
	}

	private RootConfig root;
	private RootView rootView;
	private StatusView statusView;
	private final BorderPane rootContainer = new BorderPane();

	private CenterView centerView;
	private List<IStopStart> stoppableTasks ;
	private Map<Path, List<Path>> excludedFiles;
	private volatile boolean modified, backupLastPerformedModified;
	private Map<String, Long> backupLastPerformed ;

	private ConcurrentHashMap<String, FileTree> pathFileTreeMap;

	@Override
	public void start(Stage stage) throws Exception {
		FxAlert.setParent(stage);
		Main.stage = stage;

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
		

		pathFileTreeMap = Utils.readPathFiletreeMap();
		backupLastPerformed = Utils.readBackupLastPerformedPathTimeMap();  
		
		stoppableTasks = new ArrayList<>();
		excludedFiles = Collections.synchronizedMap(new LinkedHashMap<Path, List<Path>>());

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
				modified = true;
			}
			@Override
			public void onComplete(TransferView view) {
				statusView.removeSummery(view.getSummery());
				backupLastPerformed.put("backup:"+view.getConfigView().getConfig().getSource(), System.currentTimeMillis());
				backupLastPerformedModified = true;
			}
		};

		IStartOnComplete<ConfigView> configAction = new IStartOnComplete<ConfigView>() {
			@Override
			public void start(ConfigView view) {
				if(!view.getConfig().isDisabled()) {
					List<Path> list = excludedFiles.get(view.getConfig().getSource());
					if(list == null)
						excludedFiles.put(view.getConfig().getSource(), list = new ArrayList<>());

					Config c = view.getConfig();
					WalkType w;
					if(c.getFileTree() == null) {
						FileTree ft = pathFileTreeMap.get(c.getSource().toString());
						if(ft == null) {
							w = WalkType.NEW_SOURCE;
							ft = new FileTree(c.getSource()); 
							c.setFileTree(ft);	
							pathFileTreeMap.put(c.getSource().toString(), ft);
						}
						else {
							w = WalkType.SOURCE;
							c.setFileTree(ft);
						} 
					} else {
						w = WalkType.SOURCE;
					}
					ex.execute(new WalkTask(view, list, w));
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
		if(forceModified != null || modified) {
			if(forceModified != null) {
				FileTreeWalker walker = new FileTreeWalker() {
					@Override
					public FileVisitResult file(FileTree ft, AboutFile source, AboutFile backup) {
						ft.setCopied();
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult dir(FileTree ft, AboutFile source, AboutFile backup) {
						ft.setCopied();
						return FileVisitResult.CONTINUE;
					}
				};
				forceModified.forEach(c -> c.getFileTree().walk(walker));
			}
			
			Utils.savePathFiletreeMap(pathFileTreeMap);
			Utils.saveExcludes(excludedFiles);
		}
		if(backupLastPerformedModified)
			Utils.saveBackupLastPerformedPathTimeMap(backupLastPerformed);
		System.exit(0);
	}
}
