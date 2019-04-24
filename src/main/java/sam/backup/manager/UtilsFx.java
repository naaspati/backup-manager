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
import sam.backup.manager.api.IUtilsFx;
import sam.backup.manager.config.impl.PathWrap;
import sam.di.Injector;


public final class UtilsFx {
	private static IUtilsFx fx;
	
	private UtilsFx() { }
	
	static void setFx(IUtilsFx fx) {
		UtilsFx.fx = fx;
	}

	public static Stage showStage(Window parent, Parent content) {return fx.showStage(parent, content); }
	public static void showErrorDialog(Object text, String header, Exception error) {fx.showErrorDialog(text, header, error); }
	public static FileChooser fileChooser(File expectedDir, String expectedName, String title) {return fx.fileChooser(expectedDir, expectedName, title); }
	public static void stylesheet(Parent node) {fx.stylesheet(node); }
	public static Node hyperlink(PathWrap wrap) {return fx.hyperlink(wrap); }
	public static Node hyperlink(List<PathWrap> wraps) {return fx.hyperlink(wraps); }
	public static Node button(String tooltip, String icon, EventHandler<ActionEvent> action) {return fx.button(tooltip, icon, action); }
	public static Node headerBanner(String text) {return fx.headerBanner(text); }
	public static Node bigPlaceholder(String text) {return fx.bigPlaceholder(text); }
	public static void fx(Runnable runnable) {fx.fx(runnable); }
	public static Window window(Injector injector) { return fx.window(injector); }
	public static void ensureFxThread() { fx.ensureFxThread(); }
}
