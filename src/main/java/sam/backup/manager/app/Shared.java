package sam.backup.manager.app;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerProvider;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.nopkg.EnsureSingleton;

public final class Shared {
	private static Shared INSTANCE;
	private static final EnsureSingleton singleton = new EnsureSingleton();

	static Shared init(Consumer<String> statusEater) throws Exception {
		singleton.init();
		return INSTANCE = new Shared(statusEater);
	}
	public static Shared getInstance() {
		return INSTANCE;
	}
	
	private final FileTreeFactory fileTreeFactory;
	private final ConfigManager configManager;

	private Shared(Consumer<String> s) throws Exception {
		s.accept("start Utils.init()");
		Utils.init();
		s.accept("end Utils.init()");
		
		s.accept("find ConfigManagerProvider");
		ConfigManagerProvider cmp = load(ConfigManagerProvider.class);
		cmp.load();
		this.configManager = cmp.get();
		
		s.accept("found ConfigManagerProvider: "+cmp.getClass());
		s.accept("found ConfigManager: "+configManager.getClass());
		
		s.accept("find FileTreeFactory");
		this.fileTreeFactory = load(FileTreeFactory.class);
		s.accept("found FileTreeFactory: "+fileTreeFactory.getClass());
	}
	
	private static <E> E load(Class<E> cls) {
		Iterator<E> itr = ServiceLoader.load(cls).iterator();
		if(!itr.hasNext())
			throw new IllegalStateException("no implementation found for: "+cls);
		
		E e = itr.next();
		
		if(itr.hasNext())
			throw new IllegalStateException("more than 1 implementation found for: "+cls);
		
		return e;
	}
	public ConfigManager configManager() {
		return configManager;
	}

}
