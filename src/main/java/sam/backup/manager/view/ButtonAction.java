package sam.backup.manager.view;

@FunctionalInterface
public interface ButtonAction  {
	public void handle(ButtonType type);
}
