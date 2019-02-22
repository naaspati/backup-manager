
package sam.backup.manager;

import static sam.backup.manager.SingleLoader.load;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerProvider;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.inject.Backups;
import sam.backup.manager.inject.Injector;
import sam.backup.manager.inject.Lists;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;

@SuppressWarnings("restriction")
@Singleton
public class App extends Application implements StopTasksQueue, Injector, Executor {
	public static void main(String[] args) throws URISyntaxException, IOException, SQLException {
		LauncherImpl.launchApplication(App.class, PreloaderImpl.class, args);
	}
	
	private final Logger logger = LogManager.getLogger(App.class);
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
	private Feather feather;
	private Stage stage;
	private IUtils utils;
	private IUtilsFx fx;
	private ArrayList<RunWrap> stops = new ArrayList<>();
	private ExecutorService pool; 

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
		
		s.accept("start Utils.init()");
		this.utils = load(IUtils.class, UtilsImpl.class);
		this.fx = load(IUtilsFx.class, UtilsFxImpl.class);
		s.accept("end \n  Utils: "+utils.getClass()+"\n  UtilsFx: "+fx.getClass());
		
		s.accept("find ConfigManagerProvider");
		this.configManager = load(ConfigManagerProvider.class).get();
		
		s.accept("found ConfigManager: "+configManager.getClass());
		
		configManager.load();
		
		s.accept("find FileTreeFactory");
		this.fileTreeFactory = load(FileTreeManager.class);
		s.accept("found FileTreeFactory: "+fileTreeFactory.getClass());
		
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
		
		Utils.setUtils(utils);
		UtilsFx.setFx(fx);
		
		feather = Feather.with(this);
		pool = createPool();
		notifyPreloader(new Preloader.ProgressNotification(1));
	}

	private ExecutorService createPool() {
		String s = System2.lookup("THREAD_COUNT");
		int size = 1;
		if(s == null) 
			logger.debug("THREAD_COUNT not specified, using single thread pool");
		else {
			try {
				size = Integer.parseInt(s);
				if(size < 1) {
					logger.warn("bad value for THREAD_COUNT={}, using single thread pool", size);
					size = 1;
				}
			} catch (NumberFormatException e) {
				logger.debug("failed to parse THREAD_COUNT=\"{}\"", s, e);
			}
		}
		return size == 1 ? Executors.newSingleThreadScheduledExecutor() : Executors.newFixedThreadPool(size);
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
			//FIXME
		} else {
			setView(tabs.get(0));			
		}
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

	//TODO
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
	public <E> E instance(Class<E> type) {
		return feather.instance(type);
	}
	@Override
	public <E, F extends Annotation> E instance(Class<E> type, Class<F> qualifier) {
		return feather.instance(Key.of(type, qualifier));
	}
	
	@Provides
	private Injector me() {
		return this;
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
	private Collection<? extends Config> backups() {
		return configManager.getBackups();
	}
	@Provides
	@Lists
	private Collection<? extends Config> lists() {
		return configManager.getLists();
	}
	@Provides
	@sam.backup.manager.inject.ParentWindow
	private Window stage() {
		return stage;
	}
	@Provides
	private Executor executor() {
		return this;
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
			Parent instance = feather.instance(cls);
			
			if(instance instanceof JsonRequired)
				((JsonRequired) instance).setJson(key, json);
			
			return instance; 
		}
	}
}
