package sam.backup.manager.view.backup;

import static sam.backup.manager.UtilsFx.fx;
import static sam.fx.helpers.FxClassHelper.setClass;

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
import sam.backup.manager.Utils;

class TransferRateView extends HBox {
	private final AtomicLong total = new AtomicLong();
	private volatile String totalString;
	private final AtomicLong bytesRead = new AtomicLong(), speed = new AtomicLong();

	private final Text speedT = new Text();
	private final Label totalProgressT = new Label();
	private final Label remainingTimeT = new Label();
	private final Utils utils;

	public TransferRateView(Utils utils) {
		super(5);
		this.utils = utils;
		
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
		updateTotal(ts.getTotalSize());
		
		bytesRead.addAndGet(ts.getBytesRead());
		speed.addAndGet(ts.getSpeed());
		ts.setStatusView(this);
		
	}
	public void removeSummery(TransferSummery ts) {
		updateTotal(ts.getTotalSize()*-1);
		ts.setStatusView(null);
		bytesRead.addAndGet(ts.getBytesRead()*-1);
		speed.addAndGet(ts.getSpeed()*-1);
	}
	public void updateTotal(long value) {
		totalString = "/"+ bytesToString(total.addAndGet(value));
	}
	private String bytesToString(long bytes) {
		return utils.bytesToString(bytes);
	}

	public void update(OldNewLong bytesReadOnl, OldNewLong speedOnl) {
		fx(() -> {
			totalProgressT.setText(bytesToString(bytesRead.addAndGet(bytesReadOnl.difference()))+totalString);

			if(speedOnl != null)
				speedT.setText(bytesToString(speed.addAndGet(speedOnl.difference()))+"/s");

			long l = (long)utils.divide(total.get() - bytesRead.get(), speed.get());
			remainingTimeT.setText(utils.durationToString(Duration.ofSeconds(l)));
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
