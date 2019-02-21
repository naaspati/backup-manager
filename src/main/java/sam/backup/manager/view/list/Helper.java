package sam.backup.manager.view.list;

import javax.inject.Inject;
import javax.inject.Provider;

import javafx.stage.Window;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;
import sam.backup.manager.file.api.FileTreeFactory;
import sam.backup.manager.inject.ParentWindow;

class Helper {
	final Utils utils;
	final UtilsFx fx;
	final Provider<Window> window;
	final FileTreeFactory fatory;
	
	@Inject
	public Helper(Utils utils, UtilsFx fx, @ParentWindow Provider<Window> window, FileTreeFactory fatory) {
		this.utils = utils;
		this.fx = fx;
		this.window = window;
		this.fatory = fatory;
	}
}
