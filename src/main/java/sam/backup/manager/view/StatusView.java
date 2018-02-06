package sam.backup.manager.view;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.divide;
import static sam.backup.manager.extra.Utils.durationToString;
import static sam.fx.helpers.FxHelpers.setClass;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.extra.OldNewLong;
import sam.backup.manager.extra.TransferSummery;
public class StatusView extends HBox {
	private final AtomicLong total = new AtomicLong();
	private volatile String totalString;
	private final AtomicLong bytesRead = new AtomicLong(), speed = new AtomicLong();

	private final Text speedT = new Text();
	private final Label totalProgressT = new Label();
	private final Label remainingTimeT = new Label();

	public StatusView() {
		super(5);
		setClass(this, "status-view");

		totalProgressT.setTooltip(new Tooltip("Total progress"));
		remainingTimeT.setTooltip(new Tooltip("Estimated remaining time"));

		setClass(speedT, "speed");
		setClass("text", totalProgressT, remainingTimeT);

		VBox v = new VBox(totalProgressT, remainingTimeT);
		v.setAlignment(Pos.CENTER);

		Pane pane = new Pane();
		getChildren().addAll(speedT, v);
		HBox.setHgrow(pane, Priority.ALWAYS);
		pane.setMaxWidth(Double.MAX_VALUE);
	}

	public void addSummery(TransferSummery ts) {
		updateTotal(ts.getTotal());
		
		bytesRead.addAndGet(ts.getBytesRead());
		speed.addAndGet(ts.getSpeed());
		ts.setStatusView(this);
		
	}
	public void removeSummery(TransferSummery ts) {
		updateTotal(ts.getTotal()*-1);
		ts.setStatusView(null);
		bytesRead.addAndGet(ts.getBytesRead()*-1);
		speed.addAndGet(ts.getSpeed()*-1);
	}
	public void updateTotal(long value) {
		totalString = "/"+ bytesToString(total.addAndGet(value));;
	}
	public void update(OldNewLong bytesReadOnl, OldNewLong speedOnl) {
		runLater(() -> {
			totalProgressT.setText(bytesToString(bytesRead.addAndGet(bytesReadOnl.difference()))+totalString);

			if(speedOnl != null)
				speedT.setText(bytesToString(speed.addAndGet(speedOnl.difference()))+"/s");

			long l = (long)divide(total.get() - bytesRead.get(), speed.get());
			remainingTimeT.setText(durationToString(Duration.ofSeconds(l)));
		});
	}
	public void setCompleted() {
		getChildren().setAll(speedT);
		speedT.setText("COMPLETED");
	}
	public void setCancelled() {
		getChildren().setAll(speedT);
		speedT.setText("CANCELLED");
	}

	public void remove(Button b) {
		getChildren().remove(b);
	}

}
