
package sam.backup.manager;
import static sam.backup.manager.SingleLoader.load;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codejargon.feather.Feather;
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
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigManagerProvider;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.nopkg.EnsureSingleton;

@SuppressWarnings("restriction")
public class App extends Application {
	private final Logger logger = LogManager.getLogger(App.class);
	private static final EnsureSingleton singleton = new EnsureSingleton();

	{
		singleton.init();
	}

	public static void main(String[] args) throws URISyntaxException, IOException, SQLException {
		LauncherImpl.launchApplication(App.class, PreloaderImpl.class, args);
	}

	private final AtomicBoolean stopping = new AtomicBoolean(false);
	private final List<ViewWrap> tabs = new ArrayList<>();
	private final Scene scene = new Scene(new Group(new Text("NOTHING TO VIEW")));
	private FileTreeFactory fileTreeFactory;
	private ConfigManager configManager;
	private Stage stage;

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
		notifyPreloader(new Preloader.ProgressNotification(1));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);
		BiConsumer handler = (file, e) -> FxAlert.showErrorDialog(file, "Failed to open File", e);
		FileOpenerNE.setErrorHandler(handler);
		Utils.setErrorHandler(handler);

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

	private void setView(ViewWrap tab) {
		try {
			scene.setRoot(tab.instance());
		} catch (InstantiationException | IllegalAccessException e) {
			FxAlert.showErrorDialog(tab, "failed to load view", e);
		}
	}

	/*
	 * private Node getMenubar() {
		Menu  file = new Menu("_File",
				null,
				menuitem("open app dir", e -> FileOpenerNE.openFile(new File(".")))
				//TODO menuitem("cleanup", e -> new Cleanup())
				);
		return new MenuBar(file);
	}(non-Javadoc)
	 * @see javafx.application.Application#stop()
	 */

	@Override
	public void stop() throws Exception {
		if(!stopping.compareAndSet(false, true))
			return;

		tabs.forEach(view -> {
			Object f = view.instance; 
			if(f != null && f instanceof AutoCloseable) {
				try {
					((AutoCloseable)f).close();
				} catch (Exception e1) {
					logger.error("failed to close: {}", view, e1);
				}
			}
		});

		Utils.stop();
		System.exit(0); 
	}

	@Provides
	public ConfigManager configManager() {
		return configManager;
	}
	@Provides
	public FileTreeFactory filtreeFactory() {
		return fileTreeFactory;
	}
	
	private class ViewWrap {
		final String title;
		final Class<? extends Parent> cls;
		final JSONObject json;
		Parent instance;

		@SuppressWarnings({ "unchecked"})
		public ViewWrap(String title, JSONObject json) throws ClassNotFoundException, JSONException {
			this.title = json.optString("title", title);
			this.cls = (Class<? extends Parent>) Class.forName(json.getString("class"));
			this.json = json;
		}

		@Provides
		public JSONObject json() {
			return json;
		}
		
		@Provides
		@Title
		public String title() {
			return title;
		}
		
		@Override
		public String toString() {
			return json.toString();
		}

		public Parent instance() throws InstantiationException, IllegalAccessException {
			if(instance != null)
				return instance;

			instance = Feather.with(this, App.this, configManager).instance(cls);
			json.put("instance", instance.toString());
			
			return instance; 
		}
	}
}
