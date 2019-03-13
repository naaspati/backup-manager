package sam.backup.manager.file;

interface WithId {
	int getId();
	
	public static int id(Object o) {
		return ((WithId)o).getId();
	}
}
