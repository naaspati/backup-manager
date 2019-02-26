package sam.backup.manager.view.backup;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import sam.backup.manager.Backups;
import sam.backup.manager.Injector;
import sam.backup.manager.JsonRequired;
import sam.backup.manager.SelectionListener;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.view.Deleter;
import sam.nopkg.EnsureSingleton;
import sam.reference.WeakAndLazy;

@Singleton
public class BackupViews extends BorderPane implements JsonRequired, SelectionListener {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	
//	private static final Logger LOGGER = Utils.getLogger(ConfigManager.class);
	private final VBox root = new VBox();
	private final Collection<? extends Config> backups;
	private final Provider<Injector> injector;

	@Inject
	public BackupViews(Provider<Injector> injector, FileTreeManager factory, @Backups Collection<? extends Config> backups) {
		this.backups = backups;
		this.injector = injector;
		
		setCenter(root);
		singleton.init();
	}
	
	private boolean init;
	
	@Override
	public void selected() {
		if(init)
			return;
		
		init = true;
		FileTreeManager fac = instance(FileTreeManager.class);
		WeakAndLazy<Deleter> deleter = new WeakAndLazy<>(Deleter::new);
		Provider<Deleter> deleter2 = deleter::get;
		
		backups.forEach(c -> root.getChildren().add(new BackupView(c, fac, deleter2)));
	}
	private <E> E instance(Class<E> cls) {
		return injector.get().instance(cls);
	}

	@Override
	public void setJson(String key, JSONObject json) {
		String title = json.optString("title");
		
		if(title != null)
			setTop(UtilsFx.headerBanner(title));
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
