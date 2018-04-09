package sam.backup.manager;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.getBackupLastPerformed;
import static sam.backup.manager.extra.Utils.putBackupLastPerformed;
import static sam.backup.manager.extra.Utils.readFiletree;
import static sam.backup.manager.extra.Utils.run;
import static sam.backup.manager.extra.Utils.saveFiletree;
import static sam.backup.manager.extra.Utils.showErrorDialog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.AboutDriveView;
import sam.backup.manager.config.view.ConfigView;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.State;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.transfer.TransferView;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;

public class ConfigManager implements IStartOnComplete<ConfigView> {
	private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
	private final ViewSwitcher centerView;
	private final StatusView statusView;
	private final AboutDriveView aboutDriveView;
	
	private static volatile ConfigManager instance;

	public static ConfigManager getInstance() {
		return instance;
	}
	static void init(StatusView statusView, AboutDriveView aboutDriveView, ViewSwitcher centerView, RootConfig root, Collection<IStopStart> stoppableTasks) {
		instance = new ConfigManager(statusView, aboutDriveView, centerView, root, stoppableTasks);
	}
	private ConfigManager(StatusView statusView, AboutDriveView aboutDriveView, ViewSwitcher centerView, RootConfig root, Collection<IStopStart> stoppableTasks) {
		this.centerView = centerView;
		this.statusView = statusView;
		this.aboutDriveView = aboutDriveView;

		runLater(() -> Arrays.stream(root.getBackups())
				.map(c -> new ConfigView(c, this, getBackupLastPerformed("backup:"+c.getSource())))
				.peek(stoppableTasks::add).forEach(centerView::add)
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
			aboutDriveView.refreshSize();
			try {
				saveFiletree(view.getConfig(), true);
			} catch (IOException e) {
				showErrorDialog(null, "failed to save TreeFile: ", e);	
			}
		}
	};

	@Override
	public void start(ConfigView view) {
		if(!view.getConfig().isDisabled()) {
			Config c = view.getConfig();

			if(c.getDepth() <= 0) {
				runLater(() -> view.finish("Walk failed: \nbad value for depth: "+c.getDepth(), true));
				return;
			}
			if(c.getFileTree() == null) {
				FileTree ft;
				try {
					ft = readFiletree(c, true);
				} catch (IOException e) {
					showErrorDialog(null, "failed to read TreeFile: ", e);
					LOGGER.error("failed to read TreeFile: ", e);
					return;
				}
				if(ft == null) {
					ft = new FileTree(c); 
					c.setFileTree(ft);	
				} else {
					c.setFileTree(ft);
				} 
			}
			run(new WalkTask(c, WalkMode.BOTH, view, view));
		}
	}
	@Override
	public void onComplete(ConfigView view) {
		if(view.hashBackups())
			runLater(() -> {
				TransferView v = new TransferView(view.getConfig(), view.getFilteredFileTree(), statusView, transferAction);
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