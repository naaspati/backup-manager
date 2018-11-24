package sam.backup.manager.file.db;

public class FileList extends List2<FileImpl> {

	FileList(FileImpl[] fixed) {
		super(fixed);
	}
	
	public FileImpl add(Dir parent, String filename) {
		return this.add(new FileImpl(this.nextId(), parent, filename, defaultAttrs(), defaultAttrs()));
	}
	private static Attrs defaultAttrs() {
		return new Attrs(FileTree.DEFAULT_ATTR);
	}
	@Override
	protected FileImpl[] newArray(int size) {
		return new FileImpl[size];
	}
}
