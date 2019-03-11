package sam.backup.manager.view;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.inject.Provider;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import sam.backup.manager.Injector;
import sam.backup.manager.JsonRequired;
import sam.backup.manager.SelectionListener;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.myutils.Checker;

public abstract class ViewsBase extends BorderPane implements SelectionListener, JsonRequired {
	protected final Logger logger = Utils.getLogger(getClass());
	protected final Provider<Injector> injector;
	protected String title;

	public ViewsBase(Provider<Injector> injector) {
		this.injector = injector;
		logger.debug("INIT {}", getClass());
	}

	private boolean init = false;

	@Override
	public void selected() {
		logger.debug("SELECTED {}", getClass());
		
		if(init)
			return;

		init = true;
		Injector injector = this.injector.get();
		@SuppressWarnings("unchecked")
		Collection<? extends Config> configs = injector.instance(Collection.class, annotation());
		
		setTop(UtilsFx.headerBanner(header(configs.size())));
		
		if(configs.isEmpty()) 
			setCenter(UtilsFx.bigPlaceholder(nothingFoundString()));
		 else 
			setCenter(initView(injector, configs));
	}

	@Override
	public void setJson(String key, JSONObject json) {
		this.title = json.optString("title");
		if(Checker.isEmptyTrimmed(title))
			this.title = null;
	}

	protected abstract Node initView(Injector injector, Collection<? extends Config> configs);
	protected abstract String nothingFoundString();
	protected abstract String header(int size);
	protected abstract Class<? extends Annotation> annotation();

}
