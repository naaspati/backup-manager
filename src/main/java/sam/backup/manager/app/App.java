
package sam.backup.manager.app;
import static sam.fx.helpers.FxMenu.menuitem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import com.sun.javafx.application.LauncherImpl;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.extra.Utils;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.Checker;

public class App extends Application {
	private Logger logger;

	@SuppressWarnings({ "restriction"})
	public static void main(String[] args) throws URISyntaxException, IOException, SQLException {
		LauncherImpl.launchApplication(App.class, PreloaderImpl.class, args);
	}

	private static class TabWrap {
		final String title;
		final Class<Node> cls;
		Node instance;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public TabWrap(String title, Class cls) {
			this.title = title;
			this.cls = (Class<Node>) cls;
		}

		public Node instance() throws InstantiationException, IllegalAccessException {
			if(instance != null)
				return instance;
			return instance = cls.newInstance();
		}
		@Override
		public String toString() {
			return "TabWrap [title=" + title + ", cls=" + cls + ", instance=" + instance + "]";
		}
	}

	private final BorderPane root = new BorderPane();
	private final AtomicBoolean stopping = new AtomicBoolean(false);

	private Stage stage;
	private Shared shared;
	private final List<TabWrap> tabs = new ArrayList<>();

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
		
		Consumer<String> status = s -> notifyPreloader(new PreloaderImpl.Progress(itr.nextDouble(), s));
		shared = Shared.init(status);
		logger = Utils.getLogger(App.class);
		
		status.accept("Load tabs");

		try(InputStream is = getClass().getResourceAsStream(getClass().getName()+".tabs");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
			Iterator<String> lines = reader.lines().iterator();

			while (lines.hasNext()) {
				String s = lines.next();

				if(Checker.isEmptyTrimmed(s))
					continue;
				s = s.trim();
				if(s.charAt(0) == '#')
					continue;

				int n = s.indexOf('\t');
				if(n < 0)
					logger.error("bad line: \""+s+"\"");
				else
					tabs.add(new TabWrap(s.substring(0, n).trim(), Class.forName(s.substring(n+1).trim())));
			}
		}

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

		Utils.set(stage, getHostServices());
		root.setTop(getMenubar());
		ProgressIndicator pi = new ProgressIndicator();
		pi.setMaxWidth(40);

		Scene scene = new Scene(pi);
		scene.getStylesheets().add("styles.css");

		stage.setScene(scene);
		stage.setWidth(500);
		stage.setHeight(500);
		stage.show();
		
		setView(tabs.get(0));
	}

	private void setView(TabWrap tab) {
		try {
			root.setCenter(tab.instance());
		} catch (InstantiationException | IllegalAccessException e) {
			FxAlert.showErrorDialog(tab, "failed to load view", e);
		}
	}
	
	private Node getMenubar() {
		Menu  file = new Menu("_File",
				null,
				menuitem("open app dir", e -> FileOpenerNE.openFile(new File(".")))
				//TODO menuitem("cleanup", e -> new Cleanup())
				);
		return new MenuBar(file);
	}

	@Override
	public void stop() throws Exception {
		if(!stopping.compareAndSet(false, true))
			return;

		Utils.stop();
		System.exit(0); 
	}
}
