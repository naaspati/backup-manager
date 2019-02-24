package sam.backup.manager.view;

import static sam.backup.manager.Utils.bytesToString;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.FileStore;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import sam.backup.manager.FileStoreManager;
import sam.backup.manager.SelectionListener;
import sam.fx.helpers.FxUtils;
import sam.myutils.MyUtilsException;

@Singleton
public class AboutDriveView extends VBox implements EventHandler<MouseEvent>, SelectionListener {
	private final Provider<FileStoreManager> drives;
	
	@Inject
	public AboutDriveView(Provider<FileStoreManager> drives) {
		this.drives = drives;
	}

	@Override
	public void handle(MouseEvent event) {
		if(event.getClickCount() > 1) {
			FileStoreView fs = FxUtils.find(event.getTarget(), FileStoreView.class);
			if(fs != null)
				fs.updateText();
		}
	}
	
	private boolean init = false;
	
	@Override
	public void selected() {
		if(init)
			return;
		
		init = true;
		setClass(this, "AboutDriveView");
		setOnMouseClicked(this);
		
		for(FileStore fs: drives.get().getDrives()) 
			getChildren().add(new FileStoreView(fs));
		
	}
	
	private class FileStoreView extends Label {
		private final FileStore fs;
		
		public FileStoreView(FileStore fs) {
			this.fs = fs;
			addClass(this, "FileStoreView");
			
			updateText();
		}
		private void updateText() {
			try {
				setText("Total Space: "+bytesToString(fs.getTotalSpace())+
						" | Free Space: "+bytesToString(fs.getUnallocatedSpace())
						);
			} catch (IOException e) {
				setText(MyUtilsException.toString(e));
			}
		}
	}
}
