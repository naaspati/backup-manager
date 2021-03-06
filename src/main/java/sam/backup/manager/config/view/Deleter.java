package sam.backup.manager.config.view;

import java.io.File;
import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.backup.manager.App;
import sam.backup.manager.file.db.Dir;
import sam.backup.manager.file.db.FileImpl;
import sam.backup.manager.file.db.FilteredFileTree;

public class Deleter extends Stage {
	private final TextArea view = new TextArea();
	private final Text text = new Text(), path = new Text();
	private final Set<Dir> dirs = new HashSet<>();
	private final List<FileImpl> files = new ArrayList<>();

	private Deleter() {
		super(StageStyle.UTILITY);
		initModality(Modality.WINDOW_MODAL);
		initOwner(App.getStage());

		view.setEditable(false);

		BorderPane pane = new BorderPane(view);
		pane.setBottom(text);
		pane.setTop(path);
		BorderPane.setMargin(text, new Insets(5));
		BorderPane.setMargin(path, new Insets(5));

		setOnCloseRequest(e -> {});
		setScene(new Scene(pane));
		setWidth(300);
		setHeight(500);
		show();
	}

	public static CompletableFuture<Void> process(FilteredFileTree tree) {
		Deleter d = new Deleter();
		String root = tree.getBackupPath() == null ? null : tree.getBackupPath().toString();
		d.path.setText(root);
		
		return CompletableFuture.runAsync(() -> {
			tree.walk(d);

			Iterator<FileImpl> iter = Stream.concat(
					d.files.stream(), 
					d.dirs.stream().sorted(Comparator.comparing((Dir dir) -> dir.getBackupPath().getNameCount()).reversed())
					)
					.iterator();

			StringBuilder sb = new StringBuilder();
			long time = System.currentTimeMillis() + 1000;
			int success = 0, total = 0;

			while (iter.hasNext()) {
				FileImpl fte = iter.next();
				File file = fte.getBackupPath().toFile();
				boolean b = !file.exists() || file.delete();
				if(b) fte.remove();

				String s = file.toString();
				if(root != null && s.length() > root.length() && s.startsWith(root))
					s = s.substring(root.length());

				if(b || !fte.isDirectory()) {
					if(b)
						success++;
					total++;
					sb.append(b).append("  ").append(s).append('\n');
				}

				if(System.currentTimeMillis() >= time) {
					time = System.currentTimeMillis() + 1000;
					String ss = sb.toString();
					sb.setLength(0);
					String st = success+"/"+total;
					Platform.runLater(() -> {
						d.view.appendText(ss);
						d.text.setText(st);
					});
				}
			} 
			if(sb.length() != 0) {
				String ss = sb.toString();
				String st = success+"/"+total;
				Platform.runLater(() -> {
					d.view.appendText(ss);
					d.text.setText(st);
				});
			}
			Platform.runLater(() -> d.setOnCloseRequest(e -> d.close()));
		});
	}
	@Override
	public FileVisitResult file(FileImpl ft) {
		if(ft.isBackupDeletable()) {
			dirs.add(ft.getParent());
			files.add(ft);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult dir(Dir ft) {
		return FileVisitResult.CONTINUE;
	}

}
