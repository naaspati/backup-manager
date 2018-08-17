package sam.backup.manager;

import static javafx.application.Platform.runLater;
import static sam.backup.manager.extra.Utils.showErrorDialog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.config.view.ListingView;
import sam.backup.manager.extra.IStartOnComplete;
import sam.backup.manager.extra.IStopStart;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.viewers.ViewSwitcher;
import sam.backup.manager.walk.WalkMode;
import sam.backup.manager.walk.WalkTask;

public class ListsManager implements IStartOnComplete<ListingView> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ListsManager.class);
	
	private static volatile ListsManager instance;

	public static ListsManager getInstance() {
		return instance;
	}
	static void init(RootConfig root, Collection<IStopStart> stoppableTasks, ViewSwitcher centerView) {
		instance = new ListsManager(root, stoppableTasks, centerView);
	}
	public ListsManager(RootConfig root, Collection<IStopStart> stoppableTasks, ViewSwitcher centerView) {

		List<ListingView> list = Arrays.stream(root.getLists())
				.map(c -> new ListingView(c,Utils.getBackupLastPerformed("list:"+c.getSource()), this))
				.collect(Collectors.toList());

		stoppableTasks.addAll(list);
		runLater(() -> centerView.addAllListView(list));
	}
	
	@Override
	public void start(ListingView e) {
		Config c = e.getConfig();

		if(c.getBackupConfig().getDepth() <= 0) {
			showErrorDialog(c.getSource(), "Walk failed: \nbad value for depth: "+c.getBackupConfig().getDepth(), null);
			return;
		}
		try {
			FileTree f = Utils.readFiletree(c, TreeType.LIST);
			c.setFileTree(f != null ? f : new FileTree(c));
		} catch (IOException e1) {
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
