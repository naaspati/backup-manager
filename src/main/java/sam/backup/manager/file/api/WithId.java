package sam.backup.manager.file.api;

public interface WithId {
	int getId();
	
	public static int id(Object o) {
		return ((WithId)o).getId();
	}
}
