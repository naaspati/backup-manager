package sam.backup.manager.view.backup;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import sam.backup.manager.AppConfig;
import sam.backup.manager.Backups;
import sam.backup.manager.Injector;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.view.Deleter;
import sam.backup.manager.view.ViewsBase;
import sam.nopkg.EnsureSingleton;
import sam.reference.WeakAndLazy;

@Singleton
public class BackupViews extends ViewsBase {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	
	@Inject
	public BackupViews(Provider<Injector> injector) {
		super(injector);
		singleton.init();
	}
	
	@Override
	protected Node initView(Injector injector, Collection<? extends Config> configs) {
		FileTreeManager fac = injector.instance(FileTreeManager.class);
		WeakAndLazy<Deleter> deleter = new WeakAndLazy<>(Deleter::new);
		Provider<Deleter> deleter2 = deleter::get;
		boolean SAVE_EXCLUDE_LIST = Boolean.parseBoolean(injector.instance(AppConfig.class).getConfig("SAVE_EXCLUDE_LIST"));

		VBox root = new VBox();
		configs.forEach(c -> root.getChildren().add(new BackupView(c, fac, deleter2, SAVE_EXCLUDE_LIST)));
		
		return root;
	}
	
	@Override
	protected Class<? extends Annotation> annotation() {
		return Backups.class;
	}
	@Override
	protected String header(int size) {
		return (title != null ? title : "Backups") +" ("+size+")";
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
