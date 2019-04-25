package sam.backup.manager.view.backup;

import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Provider;
import javax.inject.Singleton;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import sam.backup.manager.api.AppConfig;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.view.AbstractMainView;
import sam.backup.manager.view.Deleter;
import sam.di.Injector;
import sam.nopkg.EnsureSingleton;
import sam.reference.WeakAndLazy;

@Singleton
public class BackupViews extends AbstractMainView {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{ singleton.init(); }
	
	@Override
	protected Node initView(Injector injector, Collection<Config> configs) {
		FileTreeManager fac = injector.instance(FileTreeManager.class);
		Executor executor = injector.instance(Executor.class);
		WeakAndLazy<Deleter> deleter = new WeakAndLazy<>(Deleter::new);
		Provider<Deleter> deleter2 = deleter::get;
		boolean SAVE_EXCLUDE_LIST = Boolean.parseBoolean(injector.instance(AppConfig.class).getConfig("SAVE_EXCLUDE_LIST"));

		VBox root = new VBox();
		configs.forEach(c -> root.getChildren().add(new BackupView(c, fac, executor, deleter2, SAVE_EXCLUDE_LIST)));
		
		ScrollPane scroll = new ScrollPane(root);
		scroll.setFitToHeight(true);
		scroll.setFitToWidth(true);
		return scroll;
	}
	
	@Override
	protected Collection<Config> data(ConfigManager c) {
	    return c.get(ConfigType.BACKUP);
	}
	
	@Override
	public String getTabTitle() {
	    return "Backups";
	}
	@Override
	protected String header(int size) {
		return "Backups ("+size+")";
	}
	@Override
	protected String nothingFoundString() {
		return "NO BACKUP CONFIG(s) FOUND";
	}
	/* FIXME 
	 * private final IStartOnComplete<TransferView>  transferAction = new IStartOnComplete<TransferView>() {
		@Override
		public void start(TransferView view) {
			fx.runAsync(view);
			statusView.addSummery(view.getSummery());
		}
		@Override
		public void onComplete(TransferView view) {
			statusView.removeSummery(view.getSummery());
			utils.putBackupLastPerformed("backup:"+view.getConfig().getSource(), System.currentTimeMillis());
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
				fx.runAsync(new WalkTask(c, WalkMode.BOTH, view, view));
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
	 */
}
