package sam.backup.manager.file;

import java.nio.file.Files;
import java.nio.file.Path;

import sam.backup.manager.Utils;
import sam.myutils.Checker;

class TreePaths {
	
    final int tree_id;
	final Path metaPath;
	final Path namesPath;

	public TreePaths(int tree_id, Path saveDir) {
		int filen = tree_id * 5;
		this.tree_id = tree_id;
		metaPath = saveDir.resolve(Utils.toString(filen++));
		namesPath = saveDir.resolve(Utils.toString(filen++));
	}

	public void existsValidate() throws FailedToCreateFileTree {
		if (Checker.anyMatch(f -> !Files.isRegularFile(f), metaPath, namesPath)) {
			StringBuilder sb = new StringBuilder();
			for (Path f : new Path[] { metaPath, namesPath})
				sb.append("{\"exists\": ").append(Files.isReadable(f)).append(", \"path\": \"").append(f)
				.append("\"},\n");

			sb.setLength(sb.length() - 2);

			throw new FailedToCreateFileTree(sb.toString());
		}
	}
}
