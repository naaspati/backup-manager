package sam.backup.manager.view.config;

import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.extra.Utils;
import sam.fx.helpers.FxUtils;
import sam.myutils.MyUtilsException;

public class AboutDriveView extends VBox  implements EventHandler<MouseEvent> {
	private class FileStoreView extends Label {
		private final FileStore fs;
		
		public FileStoreView(FileStore fs) {
			this.fs = fs;
			addClass(this, "FileStoreView");
			
			updateText();
		}
		private void updateText() {
			try {
				setText("Total Space: "+Utils.bytesToString(fs.getTotalSpace())+
						" | Free Space: "+Utils.bytesToString(fs.getUnallocatedSpace())
						);
			} catch (IOException e) {
				setText(MyUtilsException.toString(e));
			}
		}
	}
	

	public AboutDriveView(ConfigManager root) {
		setClass(this, "AboutDriveView");
		setOnMouseClicked(this);
		
		for(FileStore fs: FileSystems.getDefault().getFileStores()) 
			getChildren().add(new FileStoreView(fs));
		
		runLater(() -> getChildren().stream().map(FileStoreView.class::cast).forEach(FileStoreView::updateText));
	}

	@Override
	public void handle(MouseEvent event) {
		if(event.getClickCount() > 1) {
			FileStoreView fs = FxUtils.find(event.getTarget(), FileStoreView.class);
			if(fs != null)
				fs.updateText();
		}
	}
}
