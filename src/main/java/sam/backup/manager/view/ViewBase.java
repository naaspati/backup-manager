package sam.backup.manager.view;

import static sam.fx.helpers.FxClassHelper.addClass;

import java.util.List;
import java.util.concurrent.Executor;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.file.api.FileTreeManager;

public abstract class ViewBase extends BorderPane {
	protected final Config config;
	protected final Node root;
	protected final FileTreeManager manager;
	protected final Executor executor;

	public ViewBase(Config config, String styleClass, FileTreeManager manager, Executor executor) {
		this.config = config;
		this.manager = manager;
		this.executor = executor;

		if(styleClass != null)
			addClass(this, styleClass);

		List<FileTreeMeta> metas = config.getFileTreeMetas();

		if(metas.isEmpty())
			root = UtilsFx.bigPlaceholder("NO FileTreeMeta Specified");
		else 
			root = createRoot(metas);
	}

	protected abstract Node createRoot(List<FileTreeMeta> metas);



}
