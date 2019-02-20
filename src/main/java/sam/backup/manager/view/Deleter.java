package sam.backup.manager.view;

import static sam.backup.manager.Utils.fx;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import sam.backup.manager.config.api.PathWrap;
import sam.backup.manager.file.FileTreeDeleter;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.backup.manager.file.api.FilteredDir;
import sam.myutils.MyUtilsException;

public class Deleter extends BorderPane implements FileTreeWalker {
	private final TextArea view = new TextArea();
	private final Text text = new Text(), path = new Text();
	private final Set<Dir> dirs = new HashSet<>();
	private final List<FileEntity> files = new ArrayList<>();
	private final Button back = new Button("back");

	public Deleter() {
		view.setEditable(false);

		setBottom(text);
		setTop(path);
		setCenter(view);
		
		BorderPane.setMargin(text, new Insets(5));
		BorderPane.setMargin(path, new Insets(5));
	}

	public CompletableFuture<Void> start(FileTree filetree, Dir tree) {
		String root = tree.getBackupPath() == null ? null : tree.getBackupPath().toString();
		this.path.setText(root);
		
		back.setDisable(true);
		
		//FIXME switch view 
		
		return CompletableFuture.runAsync(() -> {
			try {
				tree.walk(this);

				Iterator<FileEntity> iter = Stream.concat(
						this.files.stream(), 
						this.dirs.stream().sorted(Comparator.comparingInt((Dir dir) -> dir.getBackupPath().string().length()).reversed())
						)
						.iterator();

				StringBuilder sb = new StringBuilder();
				long time = System.currentTimeMillis() + 1000;
				int success = 0, total = 0;
				
				try(FileTreeDeleter editor = filetree.getDeleter()) {
					while (iter.hasNext()) {
						FileEntity fte = iter.next();
						PathWrap p = fte.getBackupPath();
						
						if(!fte.isDirectory()) {
							try {
								editor.delete(fte, p);
								success++;
								sb.append("success").append("  ").append(p.string()).append('\n');
							} catch (IOException e) {
								sb.append("failed ").append("  ").append(p.string()).append(", error: ").append(MyUtilsException.toString(e)).append('\n');
								// TODO: handle exception
							}
							total++;	
						}

						if(System.currentTimeMillis() >= time) {
							time = System.currentTimeMillis() + 1000;
							String ss = sb.toString();
							sb.setLength(0);
							String st = success+"/"+total;
							fx(() -> {
								this.view.appendText(ss);
								this.text.setText(st);
							});
						}
					}				
				} catch (Exception e2) {
					e2.printStackTrace();
					// TODO: handle exception
				}
				
				if(sb.length() != 0) {
					String ss = sb.toString();
					String st = success+"/"+total;
					fx(() -> {
						this.view.appendText(ss);
						this.text.setText(st);
					});
				}
			} finally {
				fx(() -> back.setDisable(false));
			}
		});
	}
	@Override
	public FileVisitResult file(FileEntity ft) {
		if(ft.getStatus().isBackupDeletable()) {
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
