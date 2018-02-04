import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.Exclude;
import sam.backup.manager.config.Root;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.WalkType;
import sam.backup.manager.file.AboutFile;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.view.CenterView;
import sam.backup.manager.view.ConfigView;
import sam.backup.manager.view.CopyingView;
import sam.backup.manager.view.ListingView;
import sam.backup.manager.view.RootView;
import sam.backup.manager.view.StatusView;
import sam.fx.alert.FxAlert;
import sam.myutils.fileutils.FilesUtils;
import sam.myutils.myutils.MyUtils;

public class Main extends Application {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
			launch(args);
	}

	private Root root;
	private RootView rootView;
	private StatusView statusView;

	private final CenterView centerView = new CenterView();
	private final List<IStopStart> stoppableTasks = new ArrayList<>();
	private final List<Path> excludedFiles = Collections.synchronizedList(new ArrayList<>());
	private final Button startButton = new Button("wait while walking");
	private final Button cancelButton = new Button("Cancel");
	private volatile boolean modified, backupLastPerformedModified;
	private final Map<String, Long> backupLastPerformed = new HashMap<>();

	private ConcurrentHashMap<String, FileTree> pathFileTreeMap;

	@Override
	public void start(Stage stage) throws Exception {
		Path drive = null;
		FxAlert.setParent(stage);

		for (Path p : FileSystems.getDefault().getRootDirectories()) {
			if(Files.exists(p.resolve(".iambackup"))) {
				drive = p;
				break;
			}
		}
		if(drive == null) {
			twoMessages(stage, "Drive Not Found", "drive must contain file: .iambackup");
			return;
		}
		try {
			Path p = Paths.get("path-filetree.dat");
			pathFileTreeMap = Files.notExists(p) ? new ConcurrentHashMap<>() : FilesUtils.readObjectFromFile(p);
		} catch (Exception e) {
			twoMessages(stage, "Failed to read: path-filetree.dat", MyUtils.exceptionToString(e));
			return;
		}
		Path configPath = Paths.get("config.json");
		if(Files.notExists(configPath)) {
			twoMessages(stage, "config.json not found", "");
			return;
		}

		try {
			Reader r = Files.newBufferedReader(configPath);
			root = new Gson().fromJson(r, Root.class);
			root.setDrive(drive);
			root.setRoot();
			r.close();
		} catch (Exception e) {
			twoMessages(stage, "failed to read: "+configPath, MyUtils.exceptionToString(e));
			return;
		}
		if(!root.hasBackups() && !root.hasLists()) {
			twoMessages(stage, "No tasks found", "");
			return;
		}

		rootView = new RootView(this.root, this);
		statusView = new StatusView(startButton, cancelButton);
		startButton.setDisable(true);
		cancelButton.setVisible(false);

		cancelButton.setOnAction(e -> {
			stoppableTasks.forEach(IStopStart::stop);
			cancelButton.setVisible(false);
			//TODO after cancel what to do with start 
		});
		
		BorderPane rootBorderpane = new BorderPane(centerView, rootView, null, statusView, null);

		Scene scene = new Scene(rootBorderpane);
		scene.getStylesheets().add("style.css");

		stage.setScene(scene);
		stage.setWidth(500);
		stage.setHeight(500);
		stage.show();
		
		try {
			Path p = Paths.get("backup-last-performed.txt");
			 if(Files.exists(p)) {
				 Files.lines(p)
					.map(s -> s.split("\t"))
					.filter(s -> s.length == 2)
					.forEach(s -> {
						try {
							backupLastPerformed.put(s[0], Long.parseLong(s[1]));
						} catch (NumberFormatException e) {}
					});
			 }
		} catch (IOException e) {}

		processConfigs();
		processLists();
		centerView.firstClick();
	}
	private void processLists() {
		if(!root.hasLists())
			return;

		Path listPath = root.getFullBackupRoot().resolve("lists");
		if(Files.notExists(listPath)) {
			try {
				Files.createDirectories(listPath);
			} catch (IOException e) {
				System.out.println("list Walk failed: failed to create dir: "+listPath+"  error: "+MyUtils.exceptionToString(e));
				return;
			}
		}

		ExecutorService ex = Executors.newSingleThreadExecutor();
		Consumer<ListingView> start = view -> ex.execute(new WalkTask(view));
		Consumer<ListingView> whenSaved = view -> {
			backupLastPerformed.put("list:"+view.getConfig().getSource(), System.currentTimeMillis());
			backupLastPerformedModified = true;
		};
		
		Stream.of(root.getLists())
				.map(c -> new ListingView(c,listPath,backupLastPerformed.get("list:"+c.getSource()), start, whenSaved))
				.forEach(l -> {
					stoppableTasks.add(l);
					centerView.add(l);
				});
	}

	private void processConfigs() {
		if(!root.hasBackups())
			return;

		ExecutorService ex = Executors.newSingleThreadExecutor();

		Consumer<ConfigView> walker = view -> {
			if(!view.getConfig().isDisabled())
				ex.execute(new WalkTask(view));
		};
		Consumer<CopyingView> startCopying = view -> {
			ex.execute(view);
			statusView.addSummery(view.getSummery());
			modified = true;
		};
		Consumer<CopyingView> completeCopying = view -> {
			statusView.removeSummery(view.getSummery());
			backupLastPerformed.put("backup:"+view.getConfigView().getConfig().getSource(), System.currentTimeMillis());
			backupLastPerformedModified = true;
		}; 
		Consumer<ConfigView> walkComplete = view -> {
			if(view.getBackupFiles() == null || view.getBackupFiles().isEmpty()) {
				backupLastPerformed.put("backup:"+view.getConfig().getSource(), System.currentTimeMillis());
				backupLastPerformedModified = true;
				return;
			}
			centerView.add(new CopyingView(view, statusView, startCopying, completeCopying));
		};
		
		Stream.of(root.getBackups())
		.map(c -> new ConfigView(c, walker, walkComplete, backupLastPerformed.get("backup:"+c.getSource())))
		.peek(stoppableTasks::add).forEach(centerView::add);
	}

	@Override
	public void stop() throws Exception {
		if(modified) {
			Path p = Paths.get("path-filetree.dat");
			try {
				FilesUtils.writeObjectToFile(pathFileTreeMap, p);
				System.out.println("modified: "+p);
			} catch (IOException e) {
				FxAlert.showErrorDialog(null, "failed to write: "+p, e);
			}
			try {
				List<String> list = excludedFiles.stream().map(String::valueOf).distinct().collect(Collectors.toList());
				list.add(0, "\n"+LocalDateTime.now()+"\n-----------------------\n");

				Files.write(Paths.get("excluded-files.txt"), list, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				System.out.println("modified: excluded-files.txt");
			} catch (IOException e) {
				System.out.println("failed to save: excluded-files.txt");
				e.printStackTrace();
			}
		}

		if(backupLastPerformedModified) {
			try {
				StringBuilder sb = new StringBuilder();
				Path p2 = Paths.get("backup-last-performed.txt");
				backupLastPerformed.forEach((s,t) -> sb.append(s).append('\t').append(t).append('\n'));
				Files.write(p2, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {}
		}
		System.exit(0);
	}

	private void twoMessages(Stage stage, String s1, String s2) {
		Text t1 = new Text(s1);
		Text t2 = new Text(s2);

		t1.setFont(Font.font("Consolas", 40));
		t2.setFont(Font.font("Consolas"));

		VBox box = new VBox(2, t1, t2);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(20));

		stage.setScene(new Scene(box));
		stage.show();
	}
	class WalkTask implements Runnable {
		private final ConfigView configView;
		private final ListingView listView;
		private final Config config;
		private volatile  long sourceSize, targetSize;
		private volatile int sourceFileCount, sourceDirCount, targetFileCount, targetDirCount;
		private final boolean listWalk;
		private final ICanceler canceler;
		private Predicate<Path> sourceExcluder, targetExcluder;
		

		public WalkTask(ConfigView view) {
			configView = view;
			canceler = view;
			config = view.getConfig();
			listView = null;
			listWalk = false;
			prepareExcluders();
		}
		public WalkTask(ListingView lv) {
			configView = null;
			listView = lv;
			canceler = lv;
			config = lv.getConfig();
			listWalk = true;
			prepareExcluders();
		}
		private void prepareExcluders() {
			sourceExcluder = createExcluder(Main.this.root.getSourceExcluder(), config.getSourceExcluder());
			targetExcluder = createExcluder(Main.this.root.getTargetExcluder(), config.getTargetExcluder());
		}
		private Predicate<Path> createExcluder(Exclude  rootExcluder, Exclude configExcluder) {
			if(rootExcluder == null && configExcluder == null)
				return (p -> false);
			if(rootExcluder == null)
				return (configExcluder::exclude);
			if(configExcluder == null)
				return (rootExcluder::exclude);
			
			return p -> configExcluder.exclude(p) || rootExcluder.exclude(p);
		}
		@Override
		public void run() {
			if(canceler.isCancelled())
				return;

			Path root = config.getSource();

			if(Files.notExists(root)) {
				if(listWalk)
					return;
				configView.disable();
				return;
			}

			FileTree tree = listWalk ? new FileTree(root, true) : pathFileTreeMap.get(config.getSource().toString());
			WalkType walkType =  listWalk ? WalkType.LIST : tree == null ? WalkType.NEW_SOURCE : WalkType.SOURCE;
			
			if(tree == null)
				tree = new FileTree(root, true);

			boolean sourceWalkFailed = true;
			pathFileTreeMap.put(config.getSource().toString(), tree);

			try {
				if(configView == null || !configView.isSourceCompleted())
					walk(tree, root, sourceExcluder, walkType);
				if(listWalk) {
					listView.setFileTree(tree);
					return;
				}
				configView.setSourceWalkCompleted();
				sourceWalkFailed = false;
				if(canceler.isCancelled()) return;
				walk(tree, config.getTarget(), targetExcluder, WalkType.BACKUP);
			} catch (IOException e) {
				boolean s = sourceWalkFailed;
				runLater(() -> configView.setError(s ? "Source walk failed: "+config.getSource() : "Target walk failed: "+config.getTarget(), e));
				configView.disable();
				return;
			}

			FileTree tree2 = tree;

			runLater(() -> {
				configView.setSourceSizeFileCount(sourceSize, sourceFileCount);
				configView.setSourceDirCount(sourceDirCount);
				configView.setTargetSizeFileCount(targetSize, targetFileCount);
				configView.setTargetDirCount(targetDirCount);
				if(!config.isDisabled())
					configView.setFileTree(tree2, Main.this.root.isOnlyExistsCheck() || config.isOnlyExistsCheck());
			});

		}

		private void walk(FileTree tree, Path root, Predicate<Path> excluder, WalkType walkType) throws IOException {
			if(Files.notExists(root)) {
				if(walkType == WalkType.BACKUP) {
					runLater(() -> {
						configView.setTargetSizeFileCount(targetSize, targetFileCount);
						configView.setTargetDirCount(targetDirCount);
					});
				}
				return;
			}

			int start = root.getNameCount();

			Files.walkFileTree(root , new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if(canceler.isCancelled())
						return FileVisitResult.TERMINATE;

					if(excluder.test(file)){
						excludedFiles.add(file);
						return FileVisitResult.CONTINUE;
					}

					AboutFile af = null;

					if(!listWalk) {
						af = new AboutFile(attrs);

						if(walkType == WalkType.BACKUP) {
							targetSize += af.getSize();
							targetFileCount++;
							runLater(() -> configView.setTargetSizeFileCount(targetSize, targetFileCount));
						} else {
							sourceSize += af.getSize();
							sourceFileCount++;

							runLater(() -> configView.setSourceSizeFileCount(sourceSize, sourceFileCount));
						}
					}
					else  {
						sourceFileCount++;
						runLater(() -> listView.setFileCount(sourceFileCount));
					}
					tree.addFile(file.subpath(start, file.getNameCount()), file, af, walkType);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if(canceler.isCancelled())
						return FileVisitResult.TERMINATE;

					if(excluder.test(dir)) {
						excludedFiles.add(dir);
						return FileVisitResult.SKIP_SUBTREE;
					}

					int end = dir.getNameCount();
					if(end != start) {
						if(!listWalk) {
							if(walkType == WalkType.BACKUP) {
								targetDirCount++;
								runLater(() -> configView.setTargetDirCount(targetDirCount));
							} else {
								sourceDirCount++;
								runLater(() -> configView.setSourceDirCount(sourceDirCount));
							}
						} else {
							sourceDirCount++;
							runLater(() -> listView.setDirCount(sourceDirCount));
						}
						
						tree.addDirectory(dir.subpath(start, end), dir, listWalk ? null : new AboutFile(attrs), walkType);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}

	}

}
