package sam.backup.manager.view;

import static sam.backup.manager.Utils.bytesToString;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.FileStore;

import javax.inject.Singleton;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import sam.backup.manager.api.FileStoreManager;
import sam.backup.manager.api.HasTitle;
import sam.backup.manager.api.SelectionListener;
import sam.di.Injector;
import sam.fx.helpers.FxUtils;
import sam.myutils.MyUtilsException;

@Singleton
public class AboutDriveView extends VBox implements EventHandler<MouseEvent>, SelectionListener, HasTitle {
    
    @Override
    public String getTabTitle() {
        return "Drive Properties";
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
	private FileStoreManager fsm;
	
	@Override
	public void selected() {
		if(init)
			return;
		
		init = true;
		setClass(this, "AboutDriveView");
		setOnMouseClicked(this);
		
		if(fsm == null)
		    fsm = Injector.getInstance().instance(FileStoreManager.class);
		
		for(FileStore fs: this.fsm.getDrives()) 
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
