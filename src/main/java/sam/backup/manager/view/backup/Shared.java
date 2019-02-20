package sam.backup.manager.view.backup;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.codejargon.feather.Feather;

import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.backup.manager.view.Deleter;

@Singleton
class Shared {
	final Utils utils;
	final UtilsFx fx;
	final FileTreeFactory factory;
	final Provider<Feather> feather;
	
	@Inject
	public Shared(Provider<Feather> feather,  Utils utils, UtilsFx fx, FileTreeFactory factory) {
		this.utils = utils;
		this.fx = fx;
		this.factory = factory;
		this.feather = feather;
	}
	
	Deleter deleter() {
		return feather.get().instance(Deleter.class);
	}
	
}
