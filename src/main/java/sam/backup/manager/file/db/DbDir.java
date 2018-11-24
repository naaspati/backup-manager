package sam.backup.manager.file.db;

import java.util.ArrayList;

class DbDir extends Dir {
	public final int id;
	public final int child_count;

	DbDir(FileTree root, int id, int child_count, Dir parent, String filename, Attrs source, Attrs backup){
		super(root, parent, filename, source, backup, new ArrayList<>(child_count+1));
		this.id = id;
		this.child_count  = child_count;
	}
	@Override
	public String toString() {
		return getClass().getSimpleName()+" [id=" + id + ", filename=" + filename + ", srcAttrs=" + srcAttrs + ", backupAttrs="
				+ backupAttrs + "]";
	}
}
