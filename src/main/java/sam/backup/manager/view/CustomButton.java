package sam.backup.manager.view;

import static sam.fx.helpers.FxHelpers.addClass;
import static sam.fx.helpers.FxHelpers.removeClass;
import static sam.fx.helpers.FxHelpers.setClass;

import java.util.function.Consumer;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class CustomButton extends Button {
	private volatile ButtonType type;
	private Consumer<ButtonType> action;

	public CustomButton(ButtonType type) {
		this(type, null);
	}
	public CustomButton(ButtonType type, Consumer<ButtonType> eventHandler) {
		setClass(this, "custom-btn");
		setType(type);
		setEventHandler(eventHandler);

		setOnAction(e -> {
			if(this.action != null)
				this.action.accept(this.type);
		});
	}

	public void setEventHandler(Consumer<ButtonType> eventHandler) { this.action = eventHandler; }
	public ButtonType getType() { return type; }

	public void setType(ButtonType type, String tooltip) {
		if(type == ButtonType.LOADING)
			setDisable(true);
		else if(this.type == ButtonType.LOADING)
			setDisable(false);
		
		if(type != null) removeClass(this, type.cssClass);
		addClass(this, type.cssClass);
		setText(type.text);
		this.type = type;
		setTooltip(tooltip == null ? null : new Tooltip(tooltip));
	}
	public void setType(ButtonType type) {
		setType(type, null);
	}
}
