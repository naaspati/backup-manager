package sam.backup.manager.view;

import static sam.fx.helpers.FxClassHelper.setClass;

import java.util.function.Consumer;
import java.util.stream.Stream;

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
	@SuppressWarnings("unchecked")
	public Stream<E> stream() {
		return container.getChildren().stream().map(n -> (E)n);
	}
	@SuppressWarnings("unchecked")
	public void forEach(Consumer<E> consumer) {
		container.getChildren().forEach(e -> consumer.accept((E) e));
	}
}
