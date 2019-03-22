package sam.backup.manager;

import java.io.File;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import sam.backup.manager.config.impl.PathWrap;
import sam.di.Injector;


public interface IUtilsFx {
	Stage showStage(Window parent, Parent content);
	void showErrorDialog(Object text, String header, Exception error) ;
	FileChooser fileChooser(File expectedDir, String expectedName, String title) ;
	
	void stylesheet(Parent node) ;
	Node hyperlink(PathWrap wrap) ;
	Node hyperlink(List<PathWrap> wraps) ;
	Node button(String tooltip, String icon, EventHandler<ActionEvent> action) ;
	Node headerBanner(String text);
	Node bigPlaceholder(String string);
	void fx(Runnable runnable);
	Window window(Injector injector);
	void ensureFxThread();
}
