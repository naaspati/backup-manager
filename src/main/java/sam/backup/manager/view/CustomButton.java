package sam.backup.manager.view;

import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.removeClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class CustomButton extends Button {
	private volatile ButtonType type;
	private ButtonAction action;

	public CustomButton(ButtonType type) {
		this(type, null);
	}
	public CustomButton(ButtonType type, ButtonAction eventHandler) {
		setClass(this, "custom-btn");
		setType(type);
		setEventHandler(eventHandler);
		

		setOnAction(e -> {
			if(this.action != null)
				this.action.handle(this.type);
		});
	}

	public void setEventHandler(ButtonAction eventHandler) { this.action = eventHandler; }
	public ButtonType getType() { return type; }

	public void setType(ButtonType type, String tooltip) {
		if(type == ButtonType.LOADING)
			setDisable(true);
		else if(this.type == ButtonType.LOADING)
			setDisable(false);
		
		if(this.type != null) removeClass(this, this.type.cssClass);
		addClass(this, type.cssClass);
		setText(type.text);
		this.type = type;
		setTooltip(tooltip == null ? null : new Tooltip(tooltip));
	}
	public void setType(ButtonType type) {
		setType(type, null);
	}
}
