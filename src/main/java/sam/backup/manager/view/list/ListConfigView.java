package sam.backup.manager.view.list;

import static sam.backup.manager.Utils.millsToTimeString;
import static sam.backup.manager.UtilsFx.fx;
import static sam.backup.manager.UtilsFx.hyperlink;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.walk.WalkListener;
import sam.backup.manager.walk.WalkMode;
import sam.console.ANSI;
import sam.functions.IOExceptionFunction;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxText;
import sam.myutils.Checker;
import sam.nopkg.Junk;
import sam.reference.WeakAndLazy;

public class ListConfigView extends VBox {
	private static final Logger LOGGER =  Utils.getLogger(ListConfigView.class);
	private static final WeakAndLazy<StringBuilder> wsb = new WeakAndLazy<>(StringBuilder::new); 

	public static boolean saveWithoutAsking;

	private final Config config;  
	private final Consumer<String> textViewer;
	private final Executor executor;
	private IOExceptionFunction<FileTreeMeta, FileTree> filetree;

	public ListConfigView(Config c, Executor executor, Consumer<String> textViewer, IOExceptionFunction<FileTreeMeta, FileTree> filetree) {
		setClass(this, "listing-view");
		this.config = c;
		this.executor = executor;
		this.textViewer = textViewer;
		this.filetree = filetree;
	}

	private class PerView extends VBox implements ButtonAction, WalkListener  {
		final FileTreeMeta meta;

		private String treeText;
		private CustomButton button;
		private Text fileCountT, dirCountT;
		private int fileCount, dirCount;
		private FutureTask task;

		public PerView(FileTreeMeta meta) {
			this.meta = meta;

			Node src = hyperlink(meta.getSource());
			if(src instanceof VBox)
				((VBox) src).getChildren().forEach(h -> addClass(h, "header"));
			else
				addClass(src, "header");

			if(meta.getSource() == null || !meta.getSource().exists()) {
				getChildren().addAll(src, FxText.ofString("Last updated: "+millsToTimeString(meta.getLastModified())));
				setDisable(true);
			} else {
				button = new CustomButton(ButtonType.WALK, this);
				fileCountT = FxText.text("  Files: --", "count-text");
				dirCountT = FxText.text("  Dirs: --", "count-text");

				getChildren().addAll(src, new HBox(10, fileCountT, dirCountT), FxText.ofString("Last updated: "+millsToTimeString(meta.getLastModified())), button);
			}
		}

		@Override
		public void handle(ButtonType type) {
			switch (type) {
				case WALK:
					start();
					break;
				case CANCEL:
					task.cancel(true);
					Platform.runLater(() -> clearTask());
					break;
				case OPEN:
					textViewer.accept(treeText);
					break;
				default:
					throw new IllegalArgumentException(String.valueOf(type));
			}
		}
		
		@Override
		public void onFileFound(FileEntity ft, long size, WalkMode mode) {
			fx(() -> fileCountT.setText("  Files: "+(++fileCount)));
		}
		@Override
		public void onDirFound(Dir ft, WalkMode mode) {
			fx(() -> dirCountT.setText("  Dirs: "+(++dirCount)));
		}
		//FIXME @Override
		public void walkCompleted() {
			if(treeText == null) 
				//FIXME treeText = new FileTreeString(config.getFileTree());
			fx(() -> button.setType(ButtonType.OPEN));
		}
		
		private void start() {
			UtilsFx.ensureFxThread();
			
			treeText = null;
			 
			int depth = config.getWalkConfig().getDepth(); 
			if(depth <= 0) {
				UtilsFx.showErrorDialog(meta.getSource(), "Walk failed: \nbad value for depth: "+depth, null);
				return;
			} else if(depth == 1) {
				StringBuilder sb = wsb.get();
				sb.setLength(0);
				
				try {
					start1Depth(meta, sb);
					treeText = sb.toString();
					sb.setLength(0);
				} catch (IOException e) {
					FxAlert.showErrorDialog(meta, "failed to walk", e);
				}
			} else {
				if(isRunning())
					throw new IllegalStateException();
				try {
					FileTree f = ListConfigView.this.filetree.apply(meta);
					Callable<Void> callable = Junk.notYetImplemented();//FIXME  new WalkTask(c, WalkMode.SOURCE, e, e)
					this.task = new FutureTask<>(callable);
					executor.execute(task);
				} catch (IOException e) {
					FxAlert.showErrorDialog(meta, "failed to load filetree", e);
				}
			}
		}
		private boolean isRunning() {
			UtilsFx.ensureFxThread();
 			return task != null && !task.isCancelled() && !task.isDone() ;
		}
		private void clearTask() {
			UtilsFx.ensureFxThread();
			
			if(task == null)
				return;
			if(task.isCancelled() || task.isDone())
				task = null;
			else
				throw new IllegalStateException("running");
		}
		
		//FIXME @Override
		public void walkFailed(String reason, Throwable e) {
			LOGGER.info(ANSI.red(reason));
			e.printStackTrace();
		}

		@Override
		public void stateChange(State s) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void failed(String msg, Throwable error) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startWalking(Path path) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endWalking(Path path) {
			// TODO Auto-generated method stub
			
		}
		
	}

	private static int start1Depth(FileTreeMeta meta, StringBuilder sink) throws IOException {
		PathWrap pw = meta.getSource();

		if(pw == null || pw.path() == null) 
			throw new IOException("unresolved: "+pw);

		Path root = pw.path(); 
		LOGGER.debug("1-depth walk: "+root);

		if(!Files.isDirectory(root)) 
			throw new FileNotFoundException("dir not found: "+root);
		
		String[] names = root.toFile().list();
		sink.append(pw.string()) .append("\n |");
		
		if(Checker.isEmpty(names))
			return 0;

		for (int i = 0; i < names.length - 1; i++)
			sink.append(names[i]).append("\n |");

		sink.append(names[names.length - 1]).append('\n');
		sink.append('\n');
		return names.length;
	}
	
	public Config getConfig() {
		return config;
	}
}
