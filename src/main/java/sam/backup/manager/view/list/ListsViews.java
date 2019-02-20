package sam.backup.manager.view.list;

import java.util.Collection;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.layout.VBox;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.Lists;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;

@Singleton
public class ListsViews extends VBox {
	private static final Logger LOGGER = LogManager.getLogger(ListsViews.class);
	private final FileTreeFactory factory;
	private final Utils utils;
	private final UtilsFx fx;

	@Inject
	public ListsViews(Utils utils, UtilsFx fx, FileTreeFactory factory, @Lists Collection<? extends Config> backups) {
		this.factory = Objects.requireNonNull(factory);
		this.utils = utils;
		this.fx = fx;
		
		backups.forEach(c -> getChildren().add(new ListingView(c,utils.getBackupLastPerformed("list:"+c.getSource()))));
	}
	public void start(ListingView e) {
		Config c = e.getConfig();

		if(c.getWalkConfig().getDepth() <= 0) {
			fx.showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getWalkConfig().getDepth(), null);
			return;
		}
		try {
			FileTree f = factory.readFiletree(c, TreeType.LIST, true);
			c.setFileTree(f);
		} catch (Exception e1) {
			fx.showErrorDialog(null, "failed to read TreeFile: ", e1);
			LOGGER.error("failed to read TreeFile	", e1);
			return;
		}
		fx.runAsync(new WalkTask(c, WalkMode.SOURCE, e, e));
	}
	@Override
	public void onComplete(ListingView e) {
		utils.putBackupLastPerformed("list:"+e.getConfig().getSource(), System.currentTimeMillis());
	}
}
