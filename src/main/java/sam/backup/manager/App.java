
package sam.backup.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;
import org.codejargon.feather.Provides;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import sam.backup.manager.api.ErrorHandler;
import sam.backup.manager.api.HasTitle;
import sam.backup.manager.api.IUtils;
import sam.backup.manager.api.IUtilsFx;
import sam.backup.manager.api.SelectionListener;
import sam.backup.manager.api.StopTasksQueue;
import sam.backup.manager.api.Stoppable;
import sam.backup.manager.config.api.ConfigManager;
import sam.di.FeatherInjector;
import sam.di.Injector;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.nopkg.EnsureSingleton;

@Singleton
public class App extends Application implements StopTasksQueue, Executor, ErrorHandler {
    private static final EnsureSingleton singleton = new EnsureSingleton();
    { singleton.init(); }

    private final Logger logger = Utils.getLogger(App.class);

    private static class RunWrap {
        private final String location;
        private final Stoppable task ;

        public RunWrap(String location, Stoppable task) {
            this.location = location;
            this.task = task;
        }
    }  

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final List<Node> tabs = new ArrayList<>();
    private final StackPane center = new StackPane();
    private final BorderPane root = new BorderPane(center);
    private final Scene scene = new Scene(root);
    private Stage stage;
    private ArrayList<RunWrap> stops = new ArrayList<>();
    private ExecutorService pool;

    @Override
    public void init() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(HasTitle.class.getName().concat(".tabs"))));
        String line;

        Injector injector = Injector.init(new FeatherInjector(this));

        Utils.setUtils(injector.instance(IUtils.class));
        UtilsFx.setFx(injector.instance(IUtilsFx.class));

        injector.instance(ConfigManager.class);

        while((line = br.readLine()) != null) {
            line = line.trim();
            if(line.isEmpty() || line.charAt(0) == '#')
                continue;

            Object o = injector.instance(Class.forName(line));
            tabs.add((Node)o);
        }

        pool =  Executors.newSingleThreadExecutor(); 
        notifyPreloader(new Preloader.ProgressNotification(1));
    }
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        FxAlert.setParent(stage);
        FxPopupShop.setParent(stage);

        scene.getStylesheets().add("styles.css");

        stage.setScene(scene);
        stage.setWidth(500);
        stage.setHeight(500);
        stage.show();

        if(tabs.isEmpty()) 
            root.setCenter(UtilsFx.bigPlaceholder("NO TABS SPECIFIED"));
        else
            setView(tabs.get(0));
    }

    @Override
    public void handleError(Object msg, Object header, Throwable thrown) {
        FxAlert.showErrorDialog(msg, header, thrown);
    }

    private void setView(Node tab) {
        try {
            center.getChildren().add(tab);

            if(tab instanceof SelectionListener)
                ((SelectionListener) tab).selected();
        } catch (Exception e) {
            logger.error("failed to load view: {}", tab, e);
            FxAlert.showErrorDialog(tab, "failed to load view", e);
        }
    }

    @Override
    public void addStopable(Stoppable runnable) {
        Objects.requireNonNull(runnable);
        stops.add(new RunWrap(Thread.currentThread().getStackTrace()[2].toString(), runnable));
    }

    @Override
    public void stop() throws Exception {
        try {
            pool.shutdownNow();
            logger.debug("waiting thread to die");
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        stops.forEach(r -> {
            try {
                r.task.stop();
            } catch (Throwable e) {
                logger.error("failed to run: {}, description: {}", r.location, r.task.description(), e);
            }
        });

        System.exit(0); 
    }

    @Provides public StopTasksQueue p1() { return this;} 
    @Provides public Executor  p2() { return this;}
    @Provides public ErrorHandler  p3() { return this;}

    @Override
    public void execute(Runnable command) {
        pool.execute(command);
    }
}
