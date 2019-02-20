package sam.backup.manager;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import sam.backup.manager.config.api.PathWrap;


public interface IFxUtils extends AutoCloseable {
	Stage showStage(Window parent, Parent content) ;
	void showErrorDialog(Object text, String header, Exception error) ;
	FileChooser selectFile(File expectedDir, String expectedName, String title) ;
	
	void stylesheet(Parent node) ;
	Node hyperlink(PathWrap wrap) ;
	Node hyperlink(List<PathWrap> wraps) ;
	Node button(String tooltip, String icon, EventHandler<ActionEvent> action) ;
	void runAsync(Task runnable) ;
	void fx(Runnable runnable);
	void setErrorHandler(BiConsumer<Object, Exception> errorHandler);
}
