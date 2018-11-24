package sam.backup.manager.file.db;

public class DirList extends List2<Dir> {

	DirList(Dir[] fixed) {
		super(fixed);
	}
	public Dir add(FileTree root, Dir parent, String filename) {
		return this.add(new Dir(root, this.nextId(), parent, filename, defaultAttrs(), defaultAttrs()));
	}
	private static Attrs defaultAttrs() {
		return new Attrs(FileTree.DEFAULT_ATTR);
	}
	@Override
	protected Dir[] newArray(int size) {
		return new Dir[size];
	}
}
