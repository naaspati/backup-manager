package sam.backup.manager.view;

import java.util.stream.Stream;

import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class CustomButtonFxTest extends ApplicationTest {
	
	@Override
	public void start(Stage stage) throws Exception {
		FlowPane root = new FlowPane(Stream.of(ButtonType.values()).map(CustomButton::new).toArray(Node[]::new));
		Scene scene = new Scene(root);
		scene.getStylesheets().add("style.css");
		stage.setScene(scene);
		stage.show();
	}

	@Test
	public void test() {
	}

}
