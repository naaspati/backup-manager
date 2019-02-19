import static sam.backup.manager.extra.Utils.fx;

import javafx.scene.layout.BorderPane;
import sam.backup.manager.view.CenterViewImpl;
import sam.backup.manager.view.ConfigViews;
import sam.backup.manager.view.ListsViews;
import sam.backup.manager.view.StatusView;
import sam.backup.manager.view.TransferViewer;
import sam.backup.manager.view.ViewType;
import sam.backup.manager.view.config.AboutDriveView;
import sam.myutils.Checker;

public class AppsecondStart {
	private void secondStart() {
		fx(() -> {
			aboutDriveView = new AboutDriveView();
			rootContainer.setTop(new BorderPane(aboutDriveView, getMenubar(), null, null, null));
		});

		centerView = new CenterViewImpl();
		fx(() -> rootContainer.setCenter(centerView));

		if(Checker.isNotEmpty(configManager().getBackups())) {
			statusView = new StatusView();
			fx(() -> TransferViewer.getInstance().setStatusView(statusView));
			ConfigViews.init(statusView, aboutDriveView, centerView, configManager);
		} else 
			centerView.setStatus(ViewType.BACKUP, true);

		if(Checker.isNotEmpty(configManager.getLists()))
			ListsViews.init(configManager, centerView);

		fx(centerView::firstClick);
	}
}
