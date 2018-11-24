package sam.backup.manager.transfer;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.State.CANCELLED;
import static sam.backup.manager.extra.State.COMPLETED;
import static sam.backup.manager.extra.State.QUEUED;
import static sam.backup.manager.extra.Utils.button;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.divide;
import static sam.backup.manager.extra.Utils.millisToString;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.LongConsumer;

import org.slf4j.Logger;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.config.Config;
import sam.backup.manager.config.view.FilesView;
import sam.backup.manager.config.view.FilesViewSelector;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.State;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.db.FilteredFileTree;
import sam.backup.manager.view.ButtonAction;
import sam.backup.manager.view.ButtonType;
import sam.backup.manager.view.CustomButton;
import sam.backup.manager.view.IUpdatable;
import sam.backup.manager.view.StatusView;
import sam.fx.popup.FxPopupShop;
import sam.myutils.MyUtilsCmd;
import sam.myutils.MyUtilsPath;
import sam.string.BasicFormat;


public class TransferView extends VBox implements Runnable, IStopStart, ButtonAction, ICanceler, IUpdatable, TransferListener {
	private static final Logger LOGGER = Utils.getLogger(TransferView.class);

	private TextArea sourceTargetTa ;
	private Text currentProgressT ;
	private Text totalProgressT ;
	private Text stateText ;

	private CustomButton uploadCancelBtn, filesBtn;
	private ProgressBar currentProgressBar ;
	private ProgressBar totalProgressBar ;

	private final TransferSummery summery = new TransferSummery();

	private ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>();

	private IStartOnComplete<TransferView> startEndAction;
	private final Config config;
	private final Text filesStats = new Text();
	private final Path source, target;
	private final Function<Path, Path> sourceSubpather, targetSubpather;
	private Transferer transferer;
	private final FilteredFileTree fileTree;

	public TransferView(Config config, FilteredFileTree filesTree, StatusView statusView, IStartOnComplete<TransferView> startCompleteAction) {
		super(3);
		addClass(this, "transfer-view");
		this.startEndAction = startCompleteAction;
		this.config = config;
		this.source = config.getSource();
		this.target = config.getTarget();
		sourceSubpather = MyUtilsPath.subpather(source);
		targetSubpather = MyUtilsPath.subpather(target);
		this.fileTree = filesTree;

		uploadCancelBtn = new CustomButton(ButtonType.UPLOAD, this);
		filesBtn = new CustomButton(ButtonType.FILES, this);

		getChildren().addAll(getHeaderText(), filesStats, new HBox(5, uploadCancelBtn, filesBtn));

		this.transferer = new Transferer(config, filesTree, this, this);
		update();
	}
	public Config getConfig() {
		return config;
	}
	public ReadOnlyObjectProperty<State> stateProperty() {
		return state.getReadOnlyProperty();
	}
	@Override
	public void update() {
		if(state == null || isState(State.UPLOADING))
			return;

		transferer.update();

		uploadCancelBtn.setDisable(selectedCount()  - copiedCount() <= 0);
		if(copiedCount() == 0) filesStats.setText(String.format("files: %s (%s files)", bts(filesSelectedSize()), selectedCount()));
		else filesStats.setText(String.format("remaining: %d (%d files), total: %d (%d files)", bts(filesSelectedSize() - copiedSize()), selectedCount() - copiedCount(),  bts(filesSelectedSize()), selectedCount()));
		summery.set(transferer.getFilesSelectedSize(), transferer.getFilesCopiedSize());

		totalProgressFormat = new BasicFormat("Total Progress: {}/"+bts(summery.getTotalSize())+"  | {}/{}/"+selectedCount()+" ({})");
	}
	private Node getHeaderText() {
		Text header = new Text(String.valueOf(config.getSource()));
		setClass(header, "header");
		return header;
	}
	public TransferSummery getSummery() {
		return summery;
	}
	@Override
	public void handle(ButtonType type) {
		switch (type) {
			case UPLOAD:
				start();	
				break;
			case CANCEL:
				stop();	
				break;
			case FILES:
				FilesView.open("select files to backup", config, fileTree, FilesViewSelector.backup()).setOnCloseRequest(e -> update());	
				break;
			default:
				throw new IllegalStateException("unknown action: "+type);
		}
	}
	public void stop() {
		setState(CANCELLED);
		uploadCancelBtn.setType(ButtonType.UPLOAD);
		summery.stop();
	}

	@Override 
	public void start() {
		if(sourceTargetTa == null) {
			sourceTargetTa = new TextArea();
			currentProgressT = new Text();
			totalProgressT = new Text();
			stateText = new Text();
			currentProgressBar = new ProgressBar(0);
			totalProgressBar = new ProgressBar(0);

			sourceTargetTa.setPrefRowCount(7);
			sourceTargetTa.setEditable(false);
			sourceTargetTa.setPadding(new Insets(5));

			setClass("text", sourceTargetTa, totalProgressT, currentProgressT);

			totalProgressBar.setMaxWidth(Double.MAX_VALUE);
			currentProgressBar.setMaxWidth(Double.MAX_VALUE);
		}

		sourceTargetTa.appendText("sourceDir: "+source+"\ntargetDir: "+target+"\n\n");

		setState(QUEUED);
		startEndAction.start(this);
		uploadCancelBtn.setType(ButtonType.CANCEL);
		filesBtn.setDisable(true);
		summery.start();
	}

	@Override
	public boolean isCancelled() {
		return isState(CANCELLED);
	}
	@Override
	public void run() {
		if(isState(QUEUED))
			return;	

		setState(State.UPLOADING);
		setState(run2());
	}

	public State run2() {
		runLater(() -> getChildren().setAll(getHeaderText(),sourceTargetTa, currentProgressT, currentProgressBar, totalProgressT, totalProgressBar, uploadCancelBtn));

		summery.start();

		try {
			if(transferer.call() == CANCELLED)
				return State.CANCELLED;
		} catch (InterruptedException e) {
			return State.CANCELLED;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		summery.stop();
		return COMPLETED;
	}
	private boolean isState(State s) {
		return s == state.get();
	}
	private void setState(State state) {
		if(isState(COMPLETED))
			throw new IllegalStateException("trying to change state after COMPLETED");

		runLater(() -> {
			this.state.set(state);

			if(isState(COMPLETED))
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
				.append("\nFiles: ").append(copiedCount())
				.append("\nSize: ").append(bts(summery.getTotalSize()))
				.append("\nTime taken: ").append(millisToString(summery.getTimeTaken()))
				.append(summery.getTimeTaken() < 3000 ? "" : "\nAverage Speed: "+bts(summery.getAverageSpeed())+"/s")
				.toString());

		top.setPadding(new Insets(5,0,5,2));
		setClass(t, "completed-text");
		getChildren().addAll(top, t);

		MyUtilsCmd.beep(4);
		runLater(() -> FxPopupShop.showHidePopup("transfer completed", 1500));
		startEndAction.onComplete(this);

		if(sourceTargetTa != null)
			Utils.writeInTempDir(config, "transfer-log", null, sourceTargetTa.getText(), LOGGER);

		sourceTargetTa = null;
		currentProgressT = null;
		totalProgressT = null;
		uploadCancelBtn = null;
		currentProgressBar = null;
		totalProgressBar = null;
		totalProgressFormat = null;
		transferer = null;
		state = null;
		startEndAction = null;
		stateText = null;
	}
	@Override
	public void copyStarted(Path src, Path target) {
		runLater(() -> sourceTargetTa.appendText("src: "+sourceSubpather.apply(src)+"\ntarget: "+targetSubpather.apply(target)+"\n---------\n"));
	}
	@Override
	public void copyCompleted(Path src, Path target) { }
	@Override
	public void addBytesRead(long n) {
		if(isCancelled())
			return;

		summery.update(n);
		updateProgress();
	}

	private volatile BasicFormat totalProgressFormat;
	private volatile LongConsumer currentProgressFormat;

	private void updateProgress() {
		runLater(() -> {
			setProgressBar(currentProgressBar, bytesRead(), currentSize());
			setProgressBar(totalProgressBar, summery.getBytesRead(), summery.getTotalSize());
			currentProgressFormat.accept(bytesRead());
			totalProgressT.setText(totalProgressFormat.format(bts(copiedSize()), copiedCount(), selectedCount() - copiedCount(), speed()));
		});
	}

	private void setProgressBar(ProgressBar bar, long current, long total) {
		bar.setProgress(divide(current, total));
	}
	@Override
	public void newTask() {
		String s = "/"+bts(currentSize());
		currentProgressFormat = bytes -> currentProgressT.setText(bts(bytes)+s);
		updateProgress();
	}

	private long currentSize() {
		return transferer.getCurrentFileSize();
	}
	private String bts(long size) {
		return bytesToString(size);
	}
	private int copiedCount() { return transferer.getFilesCopiedCount(); }
	private int selectedCount() { return transferer.getFilesSelectedCount(); }
	private long copiedSize() { return transferer.getFilesCopiedSize(); }
	private long filesSelectedSize() { return transferer.getFilesSelectedSize(); }
	private String speed() { return summery.getSpeedString(); }
	private long bytesRead() { return transferer.getCurrentBytesRead(); }
}

