package sam.backup.manager.view;

import java.util.function.Consumer;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

import static sam.fx.helpers.FxHelpers.*;

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

enum ButtonType {
	CANCEL("cancel-btn", "Cancel"), 
	WALK("walk-btn", "Walk"), 
	UPLOAD("upload-btn", "Backup"), 
	OPEN("open-btn", "Open"),
	SAVE("save-btn", "Save"),
	;

	public final String cssClass, text;
	private ButtonType(String cssClass, String text) {
		this.cssClass = cssClass; 
		this.text = text;
	}
}
