package sam.backup.manager.config.view;

import java.util.concurrent.CompletableFuture;

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
import sam.backup.manager.file.db.FileTree;
import sam.backup.manager.file.db.FilteredFileTree;
import sam.console.ANSI;

public class Deleter extends Stage {
	private final TextArea view = new TextArea();
	private final Text text = new Text(), path = new Text();

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

	public static CompletableFuture<Void> process(FileTree filetree, FilteredFileTree delete) {
		Deleter d = new Deleter();
		String root = delete.getBackupPath() == null ? null : delete.getBackupPath();
		d.path.setText(root);

		if(delete.isEmpty()) {
			d.view.setText(ANSI.createUnColoredBanner("NOTHING TO DELETE"));
			return CompletableFuture.completedFuture(null);
		}

		if(delete.size() < 20) {
			filetree.backupRemove(delete, null);
			return CompletableFuture.completedFuture(null);
		}

		return CompletableFuture.runAsync(new Runnable() {
			long time = System.currentTimeMillis() + 1000;
			int success = 0, total = 0;

			@Override
			public void run() {
				StringBuilder sb = new StringBuilder();

				filetree.backupRemove(delete, (fte, b) -> {

					if(b || !fte.isDirectory()) {
						if(b)
							success++;
						total++;
						String s = fte.getBackupPath();
						if(root != null && s.length() > root.length() && s.startsWith(root))
							s = s.substring(root.length());
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
				});

				if(sb.length() != 0) {
					String ss = sb.toString();
					String st = success+"/"+total;
					Platform.runLater(() -> {
						d.view.appendText(ss);
						d.text.setText(st);
					});
				}
				Platform.runLater(() -> d.setOnCloseRequest(e -> d.close()));
			}
		});
	}
}
