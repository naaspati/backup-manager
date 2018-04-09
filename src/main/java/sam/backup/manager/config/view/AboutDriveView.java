package sam.backup.manager.config.view;

import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;
import static sam.fx.helpers.FxHelpers.text;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;

import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.extra.Utils;
import sam.myutils.MyUtils;

public class AboutDriveView extends VBox {
	private final Text sizeText;
	private RootConfig root;

	public AboutDriveView(RootConfig root) {
		setClass(this, "root-view");
		this.root = root;

		if(!RootConfig.backupDriveFound()) {
			sizeText = null;
			Text t = text("Drive Not Found", "full-path-text");
			Text t2 = text("Drive must contain file ", "drive-warning-text-1");
			Text t3 = text(".iambackup", "drive-warning-text-2");
			Text t4 = text(" (hidden or visible)", "drive-warning-text-1");

			addClass("size-text", t2,t3, t4);

			getChildren().setAll(text(""), t, new TextFlow(t2,t3, t4));
		}
		else {
			sizeText  = text("", "size-text");
			Text t = text(String.valueOf(root.getFullBackupRoot()), "full-path-text"); 
			getChildren().addAll(new Text("Backup To"), t, sizeText);
			
			runLater(this::refreshSize);
		}
	}

	public void refreshSize() {
		if(!RootConfig.backupDriveFound())
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
