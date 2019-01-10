package sam.backup.manager;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.putBackupLastPerformed;
import static sam.backup.manager.extra.Utils.run;

import java.util.Collection;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.ConfigManager;
import sam.backup.manager.config.view.AboutDriveView;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.State;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.transfer.TransferView;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;
import sam.fx.alert.FxAlert;

public class ConfigViewManager implements IStartOnComplete<ConfigView> {
//	private static final Logger LOGGER = Utils.getLogger(ConfigManager.class);
	
	private final ViewSwitcher centerView;
	private final StatusView statusView;
	// private final AboutDriveView aboutDriveView;

	private static volatile ConfigViewManager instance;

	public static ConfigViewManager getInstance() {
		return instance;
	}
	static void init(StatusView statusView, AboutDriveView aboutDriveView, ViewSwitcher centerView, ConfigManager root, Collection<IStopStart> stoppableTasks) {
		instance = new ConfigViewManager(statusView, aboutDriveView, centerView, root, stoppableTasks);
	}
	private ConfigViewManager(StatusView statusView, AboutDriveView aboutDriveView, ViewSwitcher centerView, ConfigManager root, Collection<IStopStart> stoppableTasks) {
		this.centerView = centerView;
		this.statusView = statusView;
		// this.aboutDriveView = aboutDriveView;

		runLater(() -> root.getBackups().stream()
				.map(c -> new ConfigView(c, this, Utils.getBackupLastPerformed("backup:"+c.getSource())))
				.peek(stoppableTasks::add)
				.forEach(centerView::add)
				);
	}

	private final IStartOnComplete<TransferView>  transferAction = new IStartOnComplete<TransferView>() {
		@Override
		public void start(TransferView view) {
			run(view);
			statusView.addSummery(view.getSummery());
		}
		@Override
		public void onComplete(TransferView view) {
			statusView.removeSummery(view.getSummery());
			putBackupLastPerformed("backup:"+view.getConfig().getSource(), System.currentTimeMillis());
			try {
				view.getConfig().getFileTree().save();
			} catch (Exception e) {
				FxAlert.showErrorDialog(view.getConfig()+"\n"+view.getConfig().getFileTree(), "Failed to save filetree", e);
			}
		}
	};

	@Override
	public void start(ConfigView view) {
		if(!view.getConfig().isDisabled()) {
			Config c = view.getConfig();
			if(view.loadFileTree())
				run(new WalkTask(c, WalkMode.BOTH, view, view));
		}
	}
	@Override
	public void onComplete(ConfigView view) {
		if(view.hashBackups())
			runLater(() -> {
				TransferView v = new TransferView(view.getConfig(), view.getBackupFileTree(), statusView, transferAction);
				centerView.add(v);
				v.stateProperty().addListener((p, o, n) -> {
					view.setDisable(n == State.QUEUED || n == State.UPLOADING);
					if(n == State.COMPLETED)
						view.finish("ALL COPIED", false);
				});
			});
		else {
			putBackupLastPerformed("backup:"+view.getConfig().getSource(), System.currentTimeMillis());
		}
	}
}
