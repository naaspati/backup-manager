package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;

import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import sam.backup.manager.Drive;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.extra.Utils;
import sam.fx.helpers.FxText;
import sam.myutils.MyUtils;

public class AboutDriveView extends VBox {
	private final Text sizeText;
	private RootConfig root;

	public AboutDriveView(RootConfig root) {
		setClass(this, "root-view");
		this.root = root;

		if(!Drive.exists()) {
			sizeText = null;
			Text t = FxText.of("Drive Not Found", "full-path-text");
			Text t2 = FxText.of("Drive must contain file ", "drive-warning-text-1");
			Text t3 = FxText.of(".iambackup", "drive-warning-text-2");
			Text t4 = FxText.of(" (hidden or visible)", "drive-warning-text-1");

			addClass("size-text", t2,t3, t4);

			getChildren().setAll(new Text(""), t, new TextFlow(t2,t3, t4));
		}
		else {
			sizeText  = FxText.of("", "size-text");
			Text t = FxText.of(String.valueOf(root.getFullBackupRoot()), "full-path-text"); 
			getChildren().addAll(new Text("Backup To"), t, sizeText);
			
			runLater(this::refreshSize);
		}
	}

	public void refreshSize() {
		if(!Drive.exists())
			return;
		try {
			FileStore fs = Files.getFileStore(root.getFullBackupRoot().getRoot());
			sizeText.setText("Total Space: "+Utils.bytesToString(fs.getTotalSpace())+
					" | Free Space: "+Utils.bytesToString(fs.getUnallocatedSpace())
					);
		} catch (IOException e) {
			sizeText.setText(MyUtils.exceptionToString(e));
		}
	}
}
