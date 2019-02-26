package sam.backup.manager.view.list;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import sam.backup.manager.Backups;
import sam.backup.manager.Injector;
import sam.backup.manager.SelectionListener;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.backup.manager.view.TextViewer;
import sam.functions.IOExceptionFunction;
import sam.fx.helpers.FxCss;
import sam.reference.WeakAndLazy;

@Singleton
public class ListsViews extends BorderPane implements SelectionListener {
	private static final Logger LOGGER = Utils.getLogger(ListsViews.class);

	private VBox root;
	private ScrollPane rootSp;
	private FileTreeManager factory;
	private ConfigManager cm;
	private Provider<Injector> injector;

	@Inject
	public ListsViews(Provider<Injector> injector) {
		this.injector = injector;
	}

	private boolean init = false;

	@Override
	public void selected() {
		if(init)
			return;

		init = true;
		Injector injector = this.injector.get();
		@SuppressWarnings("unchecked")
		Collection<? extends Config> configs = injector.instance(Collection.class, Backups.class);

		Node banner = UtilsFx.headerBanner("Lists"+(configs.isEmpty() ? "" : " ("+configs.size()+")"));
		
		if(configs.isEmpty()) {
			setTop(banner);
			setCenter(UtilsFx.bigPlaceholder("Nothing Specified"));
		} else {
			this.factory = injector.instance(FileTreeManager.class);
			this.cm = injector.instance(ConfigManager.class);
			Executor executor = injector.instance(Executor.class);

			CheckBox cb = new CheckBox("save without asking");
			cb.setOnAction(e -> ListConfigView.saveWithoutAsking = cb.isSelected());

			HBox buttons = new HBox(10, cb);
			buttons.setPadding(new Insets(2, 5, 2, 5));
			buttons.setBorder(FxCss.border(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new BorderWidths(1, 0, 1, 0)));
			buttons.setAlignment(Pos.CENTER_LEFT);

			root  = new VBox(2);
			rootSp = new ScrollPane(root);

			root.setFillWidth(true);
			rootSp.setFitToWidth(true);
			rootSp.setHbarPolicy(ScrollBarPolicy.NEVER);
			IOExceptionFunction<FileTreeMeta, FileTree> filetreeGetter = ftm -> {
				try {
					return ftm.loadFiletree(factory, true);
				} catch (Exception e1) {
					if(e1 instanceof IOException)
						throw (IOException)e1;
					else 
						throw new IOException(e1);
				}
			};

			configs.forEach(c -> root.getChildren().add(new ListConfigView(c, executor, this::textView, filetreeGetter)));

			setTop(new BorderPane(banner, null, null, buttons, null));
			setCenter(root);
		}

		this.injector = null;
	}
	
	private WeakAndLazy<TextViewer> wtextViewer = new WeakAndLazy<>(TextViewer::new); 
	
	private void textView(String s) {
		TextViewer ta = wtextViewer.get();
		ta.setText(s);
		
		Node node = getCenter();
		
		ta.setOnBackAction(() -> setCenter(node));
		setCenter(ta);
	} 
}
