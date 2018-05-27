package sam.backup.manager.extra;

import static sam.backup.manager.extra.Utils.bytesToString;
import static sam.backup.manager.extra.Utils.divide;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import sam.backup.manager.transfer.OldNewLong;
import sam.backup.manager.view.StatusView;

public class TransferSummery {
	private final AtomicLong lastTime = new AtomicLong();
	private final AtomicLong lastTotal = new AtomicLong();
	private final AtomicInteger speed = new AtomicInteger();
	private final AtomicLong bytesRead = new AtomicLong();
	private volatile long startTime, stoppedTime, pauseTime;

	private final AtomicLong total = new AtomicLong();
	private volatile boolean running;
	private StatusView statusView;

	public void update(long bytesRead) {
		if(!running)
			throw new IllegalStateException("not yet started");

		long timepassed = System.currentTimeMillis() - lastTime.get();
		OldNewLong bytesReadOnl = new OldNewLong(this.bytesRead.get(), this.bytesRead.addAndGet(bytesRead));

		OldNewLong speedOnl = null;

		if (timepassed >= 1000) {
			double downloaded = bytesReadOnl.getNew() - lastTotal.get();
			long old = speed.get();
			speed.set((int)((downloaded / timepassed)*1000));
			speedOnl = new OldNewLong(old, speed.get()); 

			lastTotal.set(bytesReadOnl.getNew());
			lastTime.set(System.currentTimeMillis());
		}
		if(statusView != null)
			statusView.update(bytesReadOnl, speedOnl);
	}
	public void setTotal(long value) {
		total.set(value);
	}
	public long getTotal() {
		return total.get();
	}
	public void start() {
		if(running) return;
		
		if(startTime == 0)
			startTime = System.currentTimeMillis();
		else
			pauseTime += System.currentTimeMillis() - stoppedTime;
		running = true;
	}
	public void stop() {
		running = false;
		stoppedTime = System.currentTimeMillis();
	}
	public long getStartTime() {
		return startTime;
	}

	public String getSpeedString() {
		return bytesToString(speed.get())+"/s";
	}
	public long getBytesRead() {
		return bytesRead.get();
	}
	public long getAverageSpeed() {
		return (long)(divide(total.get(), getTimePassed())*1000);
	}
	private long getTimePassed() {
		return (running ? System.currentTimeMillis() : stoppedTime) - startTime - pauseTime;
	}
	public long getTimeTaken() {
		if(running)
			throw new IllegalStateException("not yet stopped");
		return stoppedTime - startTime - pauseTime;
	}
	public int getSpeed() {
		return speed.get();
	}
	public void setStatusView(StatusView statusView) {
		this.statusView = statusView;
	}
}
