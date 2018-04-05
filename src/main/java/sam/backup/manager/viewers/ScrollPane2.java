package sam.backup.manager.viewers;

import static sam.fx.helpers.FxClassHelper.setClass;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

abstract class ScrollPane2<E extends Node> extends ScrollPane  {
	protected final VBox container = new VBox(2);

	protected ScrollPane2() {
		setClass(this, "scroll-pane2");
		setClass(container, "container");

		setContent(container);
		
		container.setFillWidth(true);
		setFitToWidth(true);
		setHbarPolicy(ScrollBarPolicy.NEVER);
	}
	public void add(E view) {
		container.getChildren().add(view);
	}
}
