package sam.backup.manager.view.backup;

import static sam.backup.manager.Utils.bytesToString;
import static sam.backup.manager.Utils.divide;

class TransferSummery {
	private volatile long lastTime ;
	private volatile long oldTotal ;
	private volatile int speed ;
	private volatile long bytesRead ;
	private volatile long startTime, stoppedTime, pauseTime;

	private volatile long totalSize ;
	private volatile boolean running;
	private TransferRateView statusView;

	public void update(long bytesRead) {
		if(!running)
			throw new IllegalStateException("not yet started");

		long timepassed = System.currentTimeMillis() - lastTime;
		OldNewLong bytesReadOnl = new OldNewLong(this.bytesRead, this.bytesRead += bytesRead);
		OldNewLong speedOnl = null;

		if (timepassed >= 1000) {
			double downloaded = bytesReadOnl.getNew() - oldTotal;
			long old = speed;
			speed = (int)((downloaded / timepassed)*1000);
			speedOnl = new OldNewLong(old, speed); 

			oldTotal = bytesReadOnl.getNew();
			lastTime = System.currentTimeMillis();
		}
		if(statusView != null)
			statusView.update(bytesReadOnl, speedOnl);
	}
	void set(long total, long completed) {
		totalSize = total;
		oldTotal = completed;
	}
	public long getTotalSize() {
		return totalSize;
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
		return bytesToString(speed)+"/s";
	}
	public long getBytesRead() {
		return bytesRead;
	}
	public long getAverageSpeed() {
		return (long)(divide(totalSize, getTimePassed())*1000);
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
		return speed;
	}
	public void setStatusView(TransferRateView statusView) {
		this.statusView = statusView;
	}
}