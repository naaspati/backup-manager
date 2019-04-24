package sam.backup.manager.view;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.apache.logging.log4j.Logger;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.api.HasTitle;
import sam.backup.manager.api.SelectionListener;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.di.Injector;

public abstract class AbstractMainView extends BorderPane implements SelectionListener, HasTitle {
	protected final Logger logger = Utils.getLogger(getClass());

	public AbstractMainView() {
		logger.debug("INIT {}", getClass());
	}

	private boolean init = false;

	@Override
	public void selected() {
		logger.debug("SELECTED {}", getClass());
		
		if(init)
			return;

		init = true;
		Injector injector = Injector.getInstance();
		@SuppressWarnings("unchecked")
		Collection<? extends Config> configs = injector.instance(Collection.class, annotation());
		
		setTop(UtilsFx.headerBanner(header(configs.size())));
		
		if(configs.isEmpty()) 
			setCenter(UtilsFx.bigPlaceholder(nothingFoundString()));
		 else 
			setCenter(initView(injector, configs));
	}

	protected abstract Node initView(Injector injector, Collection<? extends Config> configs);
	protected abstract String nothingFoundString();
	protected abstract String header(int size);
	protected abstract Class<? extends Annotation> annotation();

	public static boolean exists(FileTreeMeta f) {
		return f != null && f.getSource() != null && f.getSource().exists();
	}

}
