package sam.backup.manager.view;

import static sam.fx.helpers.FxButton.button;
import static sam.fx.helpers.FxHBox.buttonBox;
import static sam.fx.helpers.FxHBox.maxPane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import sam.myutils.Checker;

public class TextViewer extends BorderPane {
	private final TextArea ta = new TextArea();
	private final Text text = new Text();
	private Runnable onBackAction;

	public TextViewer() {
		setCenter(ta);
		Button save = button("SAVE", e -> save());
		ta.textProperty().addListener((p, o, n) -> {
			boolean b = Checker.isEmpty(n);
			save.setDisable(b);
			
			if(b)
				text.setText(null);
			else {
				text.setText(
						"chars : "+n.length()+
					    "\nlines : "+n.chars().filter(i -> i == '\n').count()
				);	
			}
		});
		
		setBottom(buttonBox(
				button("BACK", e -> onBackAction.run()),
				text,
				maxPane(),
				save
				));
		
		HBox.setMargin(text, new Insets(0, 0, 0, 20));
	}
	public void setOnBackAction(Runnable onBackAction) {
		this.onBackAction = onBackAction;
	}
	public void setText(String text) {
		ta.setText(text);
	}
	public void clear() {
		ta.clear();
	}
	private void save() {
		// TODO Auto-generated method stub
	}
}
