package sam.backup.manager.view.backup;

import static sam.backup.manager.Utils.fx;
import static sam.backup.manager.Utils.putBackupLastPerformed;
import static sam.backup.manager.Utils.runAsync;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Backups;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.extra.State;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.backup.manager.transfer.TransferView;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;
import sam.fx.alert.FxAlert;
import sam.nopkg.EnsureSingleton;

@Singleton
public class BackupViews extends BorderPane {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	
//	private static final Logger LOGGER = Utils.getLogger(ConfigManager.class);
	private final FileTreeFactory factory;
	private final ConfigManager configManager;
	private final VBox root = new VBox();

	@Inject
	public BackupViews(FileTreeFactory factory, @Backups Collection<? extends Config> backups) {
		singleton.init();
		configManager.getBackups()
		.forEach(c -> root.getChildren().add(new BackupView(c, Utils.getBackupLastPerformed("backup:"+c.getSource()))));
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
