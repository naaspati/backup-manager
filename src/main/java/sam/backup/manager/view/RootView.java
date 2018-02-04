package sam.backup.manager.view;

import static javafx.application.Platform.runLater;
import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.button;
import static sam.fx.helpers.FxHelpers.setClass;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.backup.manager.config.Root;
import sam.backup.manager.extra.Utils;
import sam.myutils.myutils.MyUtils;
public class RootView extends VBox {
	private final Text sizeText = new Text();
	private final Root root;
	
	public RootView(Root root, Application app) {
		this.root = root;
		
		setClass(this, "root-view");

		Text t = new Text(String.valueOf(root.getFullBackupRoot()));
		addClass(t, "full-path-text");

		Button b = button("close", "Delete_10px.png", e -> {
			try {
				app.stop();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		AnchorPane.setRightAnchor(b, 0d);
		b.setVisible(false); //TODO make is visible when Main is undecorated
		
		getChildren().addAll(new AnchorPane(b), new Text("Backup To"), t);
		addClass(sizeText, "size-text");
		getChildren().add(sizeText);
		
		runLater(this::refreshSize);
	}

	public void refreshSize() {
		try {
			FileStore fs = Files.getFileStore(root.getFullBackupRoot().getRoot());
			sizeText.setText("Total: "+Utils.bytesToString(fs.getTotalSpace())+
					" | Unallocated Space: "+Utils.bytesToString(fs.getUnallocatedSpace())+
					" | Usable Space: "+Utils.bytesToString(fs.getUsableSpace())
					);
		} catch (IOException e) {
			sizeText.setText(MyUtils.exceptionToString(e));
		}
	}
}
