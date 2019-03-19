package sam.backup.manager.view.backup;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
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
}
