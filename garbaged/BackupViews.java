package sam.backup.manager.view.backup;

import static sam.backup.manager.Utils.fx;
import static sam.backup.manager.Utils.putBackupLastPerformed;
import static sam.backup.manager.Utils.runAsync;

import javafx.scene.layout.BorderPane;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.extra.State;
import sam.backup.manager.transfer.TransferView;
import sam.backup.manager.view.AboutDriveView;
import sam.backup.manager.view.CenterViewImpl;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;
import sam.fx.alert.FxAlert;

public class BackupViews extends BorderPane {
//	private static final Logger LOGGER = Utils.getLogger(ConfigManager.class);
	
	private final CenterViewImpl centerView;
	private final StatusView statusView;
	// private final AboutDriveView aboutDriveView;

	private static BackupViews instance;

	public static BackupViews getInstance() {
		return instance;
	}
	static void init(StatusView statusView, AboutDriveView aboutDriveView, CenterViewImpl centerView, ConfigManager root) {
		instance = new BackupViews(statusView, aboutDriveView, centerView, root);
	}
	private BackupViews(StatusView statusView, AboutDriveView aboutDriveView, CenterViewImpl centerView, ConfigManager root) {
		this.centerView = centerView;
		this.statusView = statusView;
		// this.aboutDriveView = aboutDriveView;

		fx(() -> root.getBackups().stream()
				.map(c -> new BackupView(c, this, Utils.getBackupLastPerformed("backup:"+c.getSource())))
				.forEach(centerView::add)
				);
	}

	private final IStartOnComplete<TransferView>  transferAction = new IStartOnComplete<TransferView>() {
		@Override
		public void start(TransferView view) {
			runAsync(view);
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

	public void start(BackupView view) {
		if(!view.getConfig().isDisabled()) {
			Config c = view.getConfig();
			if(view.loadFileTree()) // FIXME
				Utils.runAsync(new WalkTask(c, WalkMode.BOTH, view, view));
		}
	}
	@Override
	public void onComplete(BackupView view) {
		if(view.hashBackups())
			fx(() -> {
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
