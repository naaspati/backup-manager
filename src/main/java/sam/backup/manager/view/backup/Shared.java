package sam.backup.manager.view.backup;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.backup.manager.inject.Injector;
import sam.backup.manager.view.Deleter;

@Singleton
class Shared {
	final Utils utils;
	final UtilsFx fx;
	final FileTreeFactory factory;
	final Provider<Injector> injector;
	
	@Inject
	public Shared(Provider<Injector> injector,  Utils utils, UtilsFx fx, FileTreeFactory factory) {
		this.utils = utils;
		this.fx = fx;
		this.factory = factory;
		this.injector = injector;
	}
	
	Deleter deleter() {
		return injector.get().instance(Deleter.class);
	}
	
}
