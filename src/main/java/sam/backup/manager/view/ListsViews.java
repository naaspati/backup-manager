package sam.backup.manager.view;

import static sam.backup.manager.extra.Utils.fx;
import static sam.backup.manager.extra.Utils.showErrorDialog;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.view.config.ListingView;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;

public class ListsViews {
	private static final Logger LOGGER = Utils.getLogger(ListsViews.class);

	private static volatile ListsViews instance;

	public static ListsViews getInstance() {
		return instance;
	}
	static void init(ConfigManager root, CenterViewImpl centerView) {
		instance = new ListsViews(root, centerView);
	}
	public ListsViews(ConfigManager root, CenterViewImpl centerView) {

		List<ListingView> list = root.getLists().stream()
				.map(c -> new ListingView(c,Utils.getBackupLastPerformed("list:"+c.getSource())))
				.collect(Collectors.toList());
		fx(() -> centerView.addAllListView(list));
	}

	public void start(ListingView e) {
		Config c = e.getConfig();

		if(c.getWalkConfig().getDepth() <= 0) {
			showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getWalkConfig().getDepth(), null);
			return;
		}
		try {
			FileTree f = Utils.readFiletree(c, TreeType.LIST, true);
			c.setFileTree(f);
		} catch (Exception e1) {
			showErrorDialog(null, "failed to read TreeFile: ", e1);
			LOGGER.error("failed to read TreeFile	", e1);
			return;
		}
		Utils.runAsync(new WalkTask(c, WalkMode.SOURCE, e, e));
	}
	@Override
	public void onComplete(ListingView e) {
		Utils.putBackupLastPerformed("list:"+e.getConfig().getSource(), System.currentTimeMillis());
	}
}
