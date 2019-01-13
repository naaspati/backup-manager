package sam.backup.manager;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.showErrorDialog;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.ConfigManager;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;

public class ListsViewManager implements IStartOnComplete<ListingView> {
	private static final Logger LOGGER = Utils.getLogger(ListsViewManager.class);

	private static volatile ListsViewManager instance;

	public static ListsViewManager getInstance() {
		return instance;
	}
	static void init(ConfigManager root, Collection<IStopStart> stoppableTasks, ViewSwitcher centerView) {
		instance = new ListsViewManager(root, stoppableTasks, centerView);
	}
	public ListsViewManager(ConfigManager root, Collection<IStopStart> stoppableTasks, ViewSwitcher centerView) {

		List<ListingView> list = root.getLists().stream()
				.map(c -> new ListingView(c,Utils.getBackupLastPerformed("list:"+c.getSource()), this))
				.collect(Collectors.toList());

		stoppableTasks.addAll(list);
		runLater(() -> centerView.addAllListView(list));
	}

	@Override
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
		Utils.run(new WalkTask(c, WalkMode.SOURCE, e, e));
	}
	@Override
	public void onComplete(ListingView e) {
		Utils.putBackupLastPerformed("list:"+e.getConfig().getSource(), System.currentTimeMillis());
	}
}
