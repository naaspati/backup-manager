package sam.backup.manager.view;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static javafx.application.Platform.runLater;
import static sam.backup.manager.enums.State.CANCELLED;
import static sam.backup.manager.enums.State.COMPLETED;
import static sam.backup.manager.enums.State.QUEUED;
import static sam.backup.manager.enums.State.RUNNING;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.divide;
import static sam.backup.manager.extra.Utils.millisToString;
import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.button;
import static sam.fx.helpers.FxHelpers.setClass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.enums.State;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.TransferSummery;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileEntity;
import sam.fx.popup.FxPopupShop;
import sam.myutils.myutils.MyUtils;
import sam.weakstore.WeakStore;


public class TransferView extends VBox implements Runnable, IStopStart, ButtonAction, ICanceler {
	private static final WeakStore<ByteBuffer> buffers = new WeakStore<>(() -> ByteBuffer.allocateDirect(2*1024*1024), true);
	private static final Logger LOGGER =  LogManager.getLogger(Utils.class);

	private final ConfigView view;

	private TextArea sourceTargetTa ;
	private Text currentProgressT ;
	private Text totalProgressT ;
	private Text stateText ;

	private CustomButton button;
	private ProgressBar currentProgressBar ;
	private ProgressBar totalProgressBar ;

	private final TransferSummery summery = new TransferSummery();
	private String totalProgressFormat;

	private int totalFilesCount;
	private AtomicInteger filesMoved ;

	private AtomicLong currentFileSize ;
	private AtomicLong currentBytesRead ;
	private volatile String currentProgressFormat;

	private volatile State state;

	private Set<Path> createdDirs ;
	private IStartOnComplete<TransferView> startEndAction;
	private final ObservableSet<FileEntity> files;
	private final ConfigView config;

	public TransferView(ConfigView view, StatusView statusView, IStartOnComplete<TransferView> startCompleteAction) {
		super(3);
		addClass(this, "transfer-view");
		this.view = view;
		this.startEndAction = startCompleteAction;
		this.config = view;

		this.files = view.getBackups();
		button = new CustomButton(ButtonType.UPLOAD, this);
		Text text = new Text();
		
		InvalidationListener listener = e -> {
			totalFilesCount = files.size();
			summery.setTotal(files.stream().mapToLong(f -> f.getSourceAttrs().getSize()).sum());
			button.setDisable(files.isEmpty());
			text.setText("Files: "+totalFilesCount+"\nSize: "+bytesToString(summery.getTotal()));
		};
		
		listener.invalidated(null);
		files.addListener(listener);
		
		getChildren().addAll(getHeaderText(), text, button);
	}

	private Node getHeaderText() {
		Text header = new Text(view.getConfig().getSourceText());
		setClass(header, "header");
		return header;
	}
	public ConfigView getConfigView() {
		return view;
	}
	public TransferSummery getSummery() {
		return summery;
	}
	@Override
	public void handle(ButtonType type) {
		if(type == ButtonType.UPLOAD)
			start();
		else if(type == ButtonType.CANCEL)
			stop();
	}
	public void stop() {
		setState(CANCELLED);
		button.setType(ButtonType.UPLOAD);
		summery.stop();
	}

	@Override 
	public void start() {
		config.setDisable(true);
		
		if(sourceTargetTa == null) {
			sourceTargetTa = new TextArea();
			currentProgressT = new Text();
			totalProgressT = new Text();
			stateText = new Text();
			currentProgressBar = new ProgressBar(0);
			totalProgressBar = new ProgressBar(0);
			filesMoved = new AtomicInteger();
			currentFileSize = new AtomicLong();
			currentBytesRead = new AtomicLong();
			createdDirs = new HashSet<>();

			totalProgressFormat = "Total Progress: %s/"+bytesToString(summery.getTotal())+"  | %s/%s/"+totalFilesCount+" (%s)";

			sourceTargetTa.setPrefRowCount(5);
			sourceTargetTa.setEditable(false);
			sourceTargetTa.setPadding(new Insets(5));

			setClass("text", sourceTargetTa, totalProgressT, currentProgressT);

			totalProgressBar.setMaxWidth(Double.MAX_VALUE);
			currentProgressBar.setMaxWidth(Double.MAX_VALUE);
		}

		setState(QUEUED);
		startEndAction.start(this);
		button.setType(ButtonType.CANCEL);
		summery.start();
	}

	@Override
	public boolean isCancelled() {
		return state == CANCELLED;
	}
	public void addBytesRead(int n) {
		if(isCancelled())
			return;

		currentBytesRead.addAndGet(n);
		summery.update(n);

		runLater(() -> {
			setProgressBar(currentProgressBar, currentBytesRead.get(), currentFileSize.get());
			setProgressBar(totalProgressBar, summery.getBytesRead(), summery.getTotal());
			currentProgressT.setText(String.format(currentProgressFormat, bytesToString(currentBytesRead.get())));
			totalProgressT.setText(String.format(totalProgressFormat, bytesToString(summery.getBytesRead()), filesMoved, totalFilesCount - filesMoved.get(), summery.getSpeedString()));
		});
	}
	private void setProgressBar(ProgressBar bar, long current, long total) {
		bar.setProgress(divide(current, total));
	}
	@Override
	public void run() {
		if(state != QUEUED)
			return;	

		setState(RUNNING);
		setState(run2());
	}
	public State run2() {
		runLater(() -> getChildren().setAll(getHeaderText(),sourceTargetTa, currentProgressT, currentProgressBar, totalProgressT, totalProgressBar, button));

		summery.start();
		filesMoved.set(0);
		ByteBuffer buffer = buffers.get();

		for (FileEntity ft : files) {
			if(isCancelled())
				return CANCELLED;

			currentFileSize.set(ft.getSourceAttrs().getCurrent().getSize());
			currentProgressFormat = "%s/"+bytesToString(currentFileSize.get());
			currentBytesRead.set(0);
			
			Path src = ft.getSourceAttrs().getPath();
			Path target = ft.getBackupAttrs().getPath();
			
			runLater(() -> {
				sourceTargetTa.appendText("src: "+src+"\ntarget: "+target+"\n");
				currentProgressBar.setProgress(0);
			});

			addBytesRead(0);
			if(copy(src, target, buffer)) {
				ft.setCopied();
				filesMoved.incrementAndGet();
			}
		}
		summery.stop();
		buffer.clear();
		buffers.add(buffer);
		return COMPLETED;
	}
	private boolean copy(final Path src, final Path target, final ByteBuffer buffer) {
		if(isCancelled()) return false;

		if(!createdDirs.contains(target.getParent())) {
			Path p = target.getParent();
			
			try {
				Files.createDirectories(p);
				createdDirs.add(p);
			} catch (Exception e) {
				LOGGER.error("failed to create dir: ", p, e);
				return false;
			}	
		}
		if(isCancelled()) return false;
		buffer.clear();
		
 		final Path temp = target.resolveSibling(target.getFileName()+".tmp");

		try(FileChannel in = FileChannel.open(src, READ);
				FileChannel out = FileChannel.open(temp, CREATE, TRUNCATE_EXISTING, WRITE)) {

			int n = 0;
			while((n = in.read(buffer)) > 0) {
				if(isCancelled()) return false;

				buffer.flip();
				out.write(buffer);
				buffer.clear();
				addBytesRead(n);
			}
			LOGGER.debug("file copied {} -> {}", src, temp);
		} catch (IOException e) {
			LOGGER.error("file copy failed {} -> {}", src, temp, e);
			return false;
		}
		try {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.debug("file renamed {} -> {}", temp, target);
		} catch (IOException e) {
			LOGGER.error("file renaming failed {} -> {}", temp, target, e);
			return false;
		}
		return true;
	}

	private void setState(State state) {
		if(this.state == COMPLETED)
			throw new IllegalStateException("trying to change state after COMPLETED");

		this.state = state;

		runLater(() -> {
			if(state == COMPLETED)
				setCompleted();

			getChildren().remove(stateText);
			if(state == QUEUED) {
				stateText.setText("QUEUED");
				getChildren().add(stateText);
			}
		});
	}
	private void setCompleted() {
		getChildren().clear();
		Pane p = new Pane();
		p.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(p, Priority.ALWAYS);
		HBox top = new HBox(getHeaderText(), p, button("close", "Delete_10px.png", e -> ((Pane)getParent()).getChildren().remove(this)));
		
		Text  t = new Text(
				new StringBuilder("COMPLETED")
				.append("\nFiles: ").append(totalFilesCount)
				.append("\nSize: ").append(bytesToString(summery.getTotal()))
				.append("\nTime taken: ").append(millisToString(summery.getTimeTaken()))
				.append(summery.getTimeTaken() < 3000 ? "" : "\nAverage Speed: "+bytesToString(summery.getAverageSpeed())+"/s")
				.toString());

		top.setPadding(new Insets(5,0,5,2));
		setClass(t, "completed-text");
		getChildren().addAll(top, t);
		
		MyUtils.beep(4);
		runLater(() -> FxPopupShop.showHidePopup("transfer completed", 1500));
		startEndAction.onComplete(this);

		sourceTargetTa = null;
		currentProgressT = null;
		totalProgressT = null;
		button = null;
		currentProgressBar = null;
		totalProgressBar = null;
		totalProgressFormat = null;
		filesMoved = null;
		currentFileSize = null;
		currentBytesRead = null;
		currentProgressFormat = null;
		state = null;
		createdDirs = null;
		startEndAction = null;
		stateText = null;
		
		runLater(() -> config.setDisable(false));
	}
}


