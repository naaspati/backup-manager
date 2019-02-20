package sam.backup.manager.view.list;

import javax.inject.Inject;
import javax.inject.Provider;

import javafx.stage.Window;
import sam.backup.manager.ParentWindow;
import sam.backup.manager.Utils;
import sam.backup.manager.UtilsFx;

class Helper {
	final Utils utils;
	final UtilsFx fx;
	final Provider<Window> window;
	
	@Inject
	public Helper(Utils utils, UtilsFx fx, @ParentWindow Provider<Window> window) {
		this.utils = utils;
		this.fx = fx;
		this.window = window;
	}
}
