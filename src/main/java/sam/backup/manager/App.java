
package sam.backup.manager;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;
import org.codejargon.feather.Feather;
import org.codejargon.feather.Key;
import org.codejargon.feather.Provides;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sun.javafx.application.LauncherImpl;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.file.api.FileTreeManager;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.Checker;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;

@SuppressWarnings("restriction")
@Singleton
public class App extends Application implements StopTasksQueue, Executor {
	public static void main(String[] args) throws URISyntaxException, IOException, SQLException {
		LauncherImpl.launchApplication(App.class, PreloaderImpl.class, args);
	}
	
	private final Logger logger = Utils.getLogger(App.class);
	private static final EnsureSingleton singleton = new EnsureSingleton();

	{
		singleton.init();
	}
	
	private static class RunWrap {
		private final String location;
		private final Runnable task ;
		
		public RunWrap(String location, Runnable task) {
			this.location = location;
			this.task = task;
		}
	}  
	
	private final AtomicBoolean stopping = new AtomicBoolean(false);
	private final List<ViewWrap> tabs = new ArrayList<>();
	private final Scene scene = new Scene(new Group(new Text("NOTHING TO VIEW")));
	private FileTreeManager fileTreeFactory;
	private ConfigManager configManager;
	private Stage stage;
	private IUtils utils;
	private IUtilsFx fx;
	private ArrayList<RunWrap> stops = new ArrayList<>();
	private ExecutorService pool;
	private Injector injector; 
	private AppConfigImpl appConfig;
	private Path configPath;

	@Override
	public void init() throws Exception {
		OfDouble itr = new OfDouble() {
			int p = 0;
			double unit = 0.01;
			@Override
			public double nextDouble() {
				return (p++) * unit;
			}
			@Override
			public boolean hasNext() {
				return true;
			}
		};

		Consumer<String> s = t -> notifyPreloader(new PreloaderImpl.Progress(itr.nextDouble(), t));
		
		if(logger.isDebugEnabled()) {
			Consumer<String> old = s;
			 s = t -> {
				logger.debug(t);
				old.accept(t);
			};
		}
		String name = getClass().getName()+".bindings.properties";
		s.accept("loading: "+name);
		@SuppressWarnings("rawtypes")
		Map<Class, Class> map = AppInitHelper.getClassesMapping(App.class, name, logger);
		s.accept("loaded: "+name);

		this.utils = AppInitHelper.instance(map, IUtils.class, UtilsImpl.class, s);
		this.fx = AppInitHelper.instance(map, IUtilsFx.class, UtilsFxImpl.class, s);
		
		Utils.setUtils(utils);
		UtilsFx.setFx(fx);
		
		this.appConfig = new AppConfigImpl();
		utils.setAppConfig(appConfig);
		
		this.configManager = AppInitHelper.instance(map, ConfigManager.class, null, s);
		this.fileTreeFactory = AppInitHelper.instance(map, FileTreeManager.class, null, s);
		
		map.keySet().removeAll(Arrays.asList(IUtils.class, IUtilsFx.class, ConfigManager.class, FileTreeManager.class));
		
		s.accept("Load tabs");

		new JSONObject(new JSONTokener(getClass().getResourceAsStream(getClass().getName()+".tabs.json"))) {
			@Override
			public JSONObject put(String key, Object value) throws JSONException {
				try {
					tabs.add(new ViewWrap(key, (JSONObject) value));
				} catch (ClassNotFoundException e) {
					throw new JSONException(String.valueOf(value), e);
				}
				return super.put(key, value);
			};
		};
		
		pool =  Executors.newSingleThreadExecutor();
		this.injector = new InjectorImpl(map);
		
		this.configPath = Junk.notYetImplemented(); //FIXME 
		notifyPreloader(new Preloader.ProgressNotification(1));
	}
	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);
		setErrorHandler();

		scene.getStylesheets().add("styles.css");

		stage.setScene(scene);
		stage.setWidth(500);
		stage.setHeight(500);
		stage.show();

		if(tabs.isEmpty()) {
			scene.setRoot(new BorderPane(fx.bigPlaceholder("NO TABS SPECIFIED")));
			return;
		}
		
		setView(tabs.get(0));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setErrorHandler() {
		BiConsumer handler = (file, e) -> FxAlert.showErrorDialog(file, "Failed to open File", e);
		FileOpenerNE.setErrorHandler(handler);
		
		Consumer c = o -> {
			if( o instanceof ErrorHandlerRequired)
				((ErrorHandlerRequired) o).setErrorHandler(handler);	
		};
		
		c.accept(utils);
		c.accept(fx);
	}

	private void setView(ViewWrap tab) {
		try {
			scene.setRoot(tab.instance());
		} catch (Exception e) {
			FxAlert.showErrorDialog(tab, "failed to load view", e);
		}
	}
	
	@Override
	public void add(Runnable runnable) {
		stops.add(new RunWrap(Thread.currentThread().getStackTrace()[2].toString(), runnable));
	}

	@Override
	public void stop() throws Exception {
		if(!stopping.compareAndSet(false, true))
			return;
		
		try {
			pool.shutdownNow();
			logger.warn("waiting thread to die");
			pool.awaitTermination(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		tabs.forEach(view -> stop(view.instance(), view));
		
		for (Object o : new Object[]{fileTreeFactory, configManager, utils, fx}) 
			stop(o, o);
		
		stops.forEach(r -> {
			try {
				r.task.run();
			} catch (Exception e) {
				logger.error("failed to run: {}", r.location, e);
			}
		});
		
		System.exit(0); 
	}

	private void stop(Object tostop, Object message) {
		if(tostop == null)
			return;
		
		try {
			if(tostop instanceof Stoppable)
				((Stoppable) tostop).stop();
		} catch (Exception e1) {
			logger.error("failed to stop: {}", message, e1);
		}
	}
	
	@Override
	public void execute(Runnable command) {
		pool.execute(command);
	}
	
	private class ViewWrap {
		final Class<? extends Parent> cls;
		final String key;
		
		JSONObject json;

		@SuppressWarnings({ "unchecked"})
		public ViewWrap(String key, JSONObject json) throws ClassNotFoundException, JSONException {
			this.key = key;
			this.cls = (Class<? extends Parent>) Class.forName(json.getString("class"));
			this.json = json;
		}
		
		@Override
		public String toString() {
			return json == null ? "ViewWrap []" : (key+":"+ json.toString());
		}
		
		public Parent instance() {
			Parent instance = injector.instance(cls);
			
			if(instance instanceof JsonRequired)
				((JsonRequired) instance).setJson(key, json);
			
			return instance; 
		}
	}
	
	private class AppConfigImpl implements AppConfig {
		public final boolean SAVE_EXCLUDE_LIST = System2.lookupBoolean("SAVE_EXCLUDE_LIST", true);
		public final Path app_data = Paths.get("app_data");
		public final Path temp_dir;
		
		public AppConfigImpl() throws IOException {
			String dt = MyUtilsPath.pathFormattedDateTime();
			String dir = Stream.of(MyUtilsPath.TEMP_DIR.toFile().list())
					.filter(s -> s.endsWith(dt))
					.findFirst()
					.orElse(null);

			if(dir != null) {
				temp_dir = MyUtilsPath.TEMP_DIR.resolve(dir);
			} else {
				int n = Utils.number(MyUtilsPath.TEMP_DIR);
				temp_dir = MyUtilsPath.TEMP_DIR.resolve((n+1)+" - "+MyUtilsPath.pathFormattedDateTime());
				Files.createDirectories(temp_dir);		
			}
		}

		@Override
		public Path appDataDir() {
			return app_data;
		}
		@Override
		public Path tempDir() {
			return temp_dir;
		}
		@Override
		public Object getConfig(ConfigName name) {
			switch (name) {
				case CONFIG_PATH_JSON: return configPath;
				case SAVE_EXCLUDE_LIST:  return SAVE_EXCLUDE_LIST;
			}
			
			throw new IllegalArgumentException();
		}
	}

	
	@SuppressWarnings({"unchecked", "rawtypes"})
	private class InjectorImpl implements Injector {
		final Feather feather;
		final Map<Class, Class> mapping ;
		final FileSystem fs = FileSystems.getDefault();	
		
		public InjectorImpl(Map<Class, Class> mapping) throws IOException, ClassNotFoundException {
			this.mapping = Checker.isEmpty(mapping) ? Collections.emptyMap() : Collections.unmodifiableMap(mapping);
			this.feather = Feather.with(this);
		}

		@Override
		public <E> E instance(Class<E> type) {
			return feather.instance(map(type));
		}
		@Override
		public <E, F extends Annotation> E instance(Class<E> type, Class<F> qualifier) {
			return feather.instance(Key.of(map(type), qualifier));
		}
		private <E> Class<E> map(Class<E> type) {
			return mapping.getOrDefault(type, type);
		}
		@Provides
		private AppConfig config() {
			return appConfig;
		}
		@Provides
		private Injector me() {
			return this;
		}
		@Provides
		private FileSystem fs() {
			return fs;
		}
		@Provides
		private ConfigManager configManager() {
			return configManager;
		}
		@Provides
		private FileTreeManager filtreeFactory() {
			return fileTreeFactory;
		}
		@Provides
		private IUtils getUtils() {
			return utils;
		}
		@Provides
		private IUtilsFx getFx() {
			return fx;
		}
		@Provides
		@Backups
		private Collection<Config> backups() {
			return configManager.get(ConfigType.BACKUP);
		}
		@Provides
		@Lists
		private Collection<Config> lists() {
			return configManager.get(ConfigType.LIST);
		}
		@Provides
		@ParentWindow
		private Window stage() {
			return stage;
		}
		@Provides
		private Executor executor() {
			return App.this;
		}
	}  
}
