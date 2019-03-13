package sam.backup.manager.file;

import java.nio.file.Files;
import java.nio.file.Path;

import sam.backup.manager.Utils;
import sam.myutils.Checker;

class TreePaths {
	
	final Path meta;
	final Path filenamesPath;
	final Path attrsPath;
	final Path remainingPath;

	public TreePaths(int tree_id, Path saveDir) {
		int filen = tree_id * 100;
		meta = saveDir.resolve(Utils.toString(filen++));
		filenamesPath = saveDir.resolve(Utils.toString(filen++));
		attrsPath = saveDir.resolve(Utils.toString(filen++));
		remainingPath = saveDir.resolve(Utils.toString(filen++));
	}

	public void existsValidate() throws FailedToCreateFileTree {
		if (Checker.anyMatch(f -> !Files.isRegularFile(f), meta, filenamesPath, attrsPath)) {
			StringBuilder sb = new StringBuilder();
			for (Path f : new Path[] { meta, filenamesPath, attrsPath })
				sb.append("{\"exists\": ").append(Files.isReadable(f)).append(", \"path\": \"").append(f)
				.append("\"},\n");

			sb.setLength(sb.length() - 2);

			throw new FailedToCreateFileTree(sb.toString());
		}
	}
}
