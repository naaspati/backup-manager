package sam.backup.manager.view;

import static sam.backup.manager.UtilsFx.fx;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;

import javax.inject.Inject;
import javax.inject.Singleton;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import sam.backup.manager.SelectionListener;
import sam.backup.manager.Utils;
import sam.fx.helpers.FxUtils;
import sam.myutils.MyUtilsException;

@Singleton
public class AboutDriveView extends VBox implements EventHandler<MouseEvent>, SelectionListener {
	private final Utils utils;
	
	private class FileStoreView extends Label {
		private final FileStore fs;
		
		public FileStoreView(FileStore fs) {
			this.fs = fs;
			addClass(this, "FileStoreView");
			
			updateText();
		}
		private void updateText() {
			try {
				setText("Total Space: "+utils.bytesToString(fs.getTotalSpace())+
						" | Free Space: "+utils.bytesToString(fs.getUnallocatedSpace())
						);
			} catch (IOException e) {
				setText(MyUtilsException.toString(e));
			}
		}
	}
	

	@Inject
	public AboutDriveView(Utils utils) {
		this.utils = utils;
	}
	@Override
	public void handle(MouseEvent event) {
		if(event.getClickCount() > 1) {
			FileStoreView fs = FxUtils.find(event.getTarget(), FileStoreView.class);
			if(fs != null)
				fs.updateText();
		}
	}
	
	private boolean init;
	
	@Override
	public void selected() {
		if(!init) {
			setClass(this, "AboutDriveView");
			setOnMouseClicked(this);
			
			for(FileStore fs: FileSystems.getDefault().getFileStores()) 
				getChildren().add(new FileStoreView(fs));
		}
		
		init = true;
		fx(() -> getChildren().stream().map(FileStoreView.class::cast).forEach(FileStoreView::updateText));
	}
}
