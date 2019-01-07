package sam.backup.manager.cleanup;

import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class Cleanup extends Dialog<String[]> {
	public Cleanup() {
		setTitle("Cleanup task");
		setHeaderText("Cleanup text");
		GridPane gp = new GridPane();
		gp.setHgap(5);
		gp.setVgap(5);
		
		TextField source = new TextField();
		TextField backup = new TextField();
		
		gp.addRow(0, new Text("source"), source);
		gp.addRow(0, new Text("backup"), backup);
		
		getDialogPane().setContent(gp);
		
	}
}
