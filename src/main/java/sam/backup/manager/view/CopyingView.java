package sam.backup.manager.view;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.State.CANCELLED;
import static sam.backup.manager.extra.State.COMPLETED;
import static sam.backup.manager.extra.State.QUEUED;
import static sam.backup.manager.extra.State.RUNNING;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.extra.ICanceler;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.State;
import sam.backup.manager.extra.TransferSummery;
import sam.backup.manager.file.FileTree;

public class CopyingView extends VBox implements Runnable, IStopStart, Consumer<ButtonType>, ReadOnlyProperty<State>, ICanceler {
	private final ConfigView view;

	private TextArea sourceTargetTa ;
	private Text currentProgressT ;
	private Text totalProgressT ;
	private Text stateText ;

	private CustomButton button;
	private ProgressBar currentProgressBar ;
	private ProgressBar totalProgressBar ;

	private final TransferSummery summery = new TransferSummery();;
	private String totalProgressFormat;

	private final int totalFilesCount;
	private AtomicInteger filesMoved ;

	private AtomicLong currentFileSize ;
	private AtomicLong currentBytesRead ;
	private volatile String currentProgressFormat;

	private volatile State state;

	private Set<Path> createdDirs ;
	private Consumer<CopyingView> startAction, completeAction;

	public CopyingView(ConfigView view, StatusView statusView, Consumer<CopyingView> whenStart, Consumer<CopyingView> whenComplete) {
		super(3);
		addClass(this, "copying-view");
		this.view = view;
		this.startAction = whenStart;
		completeAction = whenComplete;
		
		totalFilesCount = view.getBackupFiles().size();
		summery.setTotal(view.getBackupFiles().stream().map(FileTreeView::getFileTree).mapToLong(FileTree::getSourceSize).sum());
		
		button = new CustomButton(ButtonType.UPLOAD, this);
		getChildren().addAll(getHeaderText(), button, new Text("Files: "+totalFilesCount+"\nSize: "+bytesToString(summery.getTotal())));
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
	public void accept(ButtonType type) {
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

	public void start() {
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
			
			totalProgressFormat = "Total Progress: %s/"+bytesToString(summery.getTotal())+"  | %s/%s/"+view.getBackupFiles().size()+" (%s)";

			sourceTargetTa.setPrefRowCount(5);
			sourceTargetTa.setEditable(false);
			sourceTargetTa.setPadding(new Insets(5));

			setClass("text", sourceTargetTa, totalProgressT, currentProgressT);

			totalProgressBar.setMaxWidth(Double.MAX_VALUE);
			currentProgressBar.setMaxWidth(Double.MAX_VALUE);
		}

		setState(QUEUED);
		startAction.accept(this);
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
		ByteBuffer buffer = ByteBuffer.allocateDirect(2*1024*1024);

		for (FileTreeView f : view.getBackupFiles()) {
			if(isCancelled())
				return CANCELLED;

			FileTree ft = f.getFileTree();
			currentFileSize.set(ft.getSourceSize());
			currentProgressFormat = "%s/"+bytesToString(currentFileSize.get());
			currentBytesRead.set(0);
			runLater(() -> {
				sourceTargetTa.appendText("src: "+ft.getSourcePath()+"\ntarget: "+ft.getTargetPath()+"\n");
				currentProgressBar.setProgress(0);
			});

			addBytesRead(0);
			if(copy(ft, buffer)) {
				ft.setCopied();
				filesMoved.incrementAndGet();
			}
		}
		summery.stop();
		return COMPLETED;
	}
	private void setCompleted() {
		runLater(() -> {
			getChildren().clear();
			Pane p = new Pane();
			p.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(p, Priority.ALWAYS);
			HBox top = new HBox(getHeaderText(), p, button("close", "Delete_10px.png", e -> ((Pane)getParent()).getChildren().remove(this)));

			Text  t = new Text("COMPLETED"+
					"\nFiles: "+totalFilesCount+
					"\nSize: "+bytesToString(summery.getTotal())+
					"\nTime taken: "+millisToString(summery.getTimeTaken())+
					(summery.getTimeTaken() < 3000 ? "" : "\nAverage Speed: "+bytesToString(summery.getAverageSpeed())+"/s")
					);

			top.setPadding(new Insets(5,0,5,2));
			setClass(t, "completed-text");
			getChildren().addAll(top, t);
			completeAction.accept(this);
			
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
			startAction = null;
			stateText = null;
		});
	}

	private boolean copy(FileTree ft, ByteBuffer buffer) {
		if(isCancelled()) return false;

		if(!createdDirs.contains(ft.getTargetPath().getParent())) {
			try {
				Files.createDirectories(ft.getTargetPath().getParent());
				createdDirs.add(ft.getTargetPath().getParent());
			} catch (Exception e) {
				System.out.println("src: "+ft.getSourcePath()+"\ntarget: "+ft.getTargetPath()+"\n Error:"+e+"\n");
				return false;
			}	
		}

		if(isCancelled()) return false;

		buffer.clear();

		try(FileChannel in = FileChannel.open(ft.getSourcePath(), READ);
				FileChannel out = FileChannel.open(ft.getTargetPath(), CREATE, TRUNCATE_EXISTING, WRITE)) {

			int n = 0;
			while((n = in.read(buffer)) > 0) {
				if(isCancelled()) return false;

				buffer.flip();
				out.write(buffer);
				buffer.clear();
				addBytesRead(n);
			}
		} catch (IOException e) {
			System.out.println("src: "+ft.getSourcePath()+"\ntarget: "+ft.getTargetPath()+"\n Error:"+e+"\n");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private List<ChangeListener<? super State>> changeListeners;
	private List<InvalidationListener> invalidationListeners;

	private void setState(State state) {
		if(this.state == COMPLETED)
			throw new IllegalStateException("trying to change state after COMPLETED");

		State old = this.state;
		this.state = state;

		if(state == COMPLETED)
			setCompleted();

		runLater(() -> {
			getChildren().remove(stateText);
			if(state == QUEUED) {
				stateText.setText("QUEUED");
				getChildren().add(stateText);
			}

			if(changeListeners != null)
				changeListeners.forEach(c -> c.changed(this, old, state));
			if(invalidationListeners != null)
				invalidationListeners.forEach(c -> c.invalidated(this));
		});
	}

	@Override
	public void addListener(ChangeListener<? super State> listener) {
		if(changeListeners == null) changeListeners = new ArrayList<>();
		changeListeners.add(listener);	
	}

	@Override
	public void removeListener(ChangeListener<? super State> listener) {
		if(changeListeners != null) changeListeners.remove(listener);
	}
	@Override
	public State getValue() {
		return state;
	}
	@Override
	public void addListener(InvalidationListener listener) {
		if(invalidationListeners == null) invalidationListeners = new ArrayList<>();
		invalidationListeners.add(listener);
	}
	@Override
	public void removeListener(InvalidationListener listener) {
		if(invalidationListeners != null) invalidationListeners.remove(listener);
	}
	@Override
	public Object getBean() {
		return this;
	}
	@Override
	public String getName() {
		return "";
	}
}


