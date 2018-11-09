package sam.backup.manager.walk;

import static sam.backup.manager.extra.Utils.writeInTempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.Utils;
class SaveExcludeFilesList {
	public SaveExcludeFilesList(WalkMode initialWalkMode, Config config, Walker walker) {
		if(walker.excludeFilesList.isEmpty())
			return;
		StringBuilder sb = new StringBuilder();
		Path root = config.getSource();
		int count = root.getNameCount();
		sb.append(LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))).append('\n');
		sb.append(root).append('\n');

		for (Path path : walker.excludeFilesList) {
			if(path.startsWith(root))
				sb.append("   ").append(path.subpath(count, path.getNameCount())).append('\n');
			else
				sb.append(path).append('\n');
		}
		sb.append("\n\n-------------------------------------------------\n\n");
		writeInTempDir(config, "excluded-files-"+config.getName(), initialWalkMode.toString(), sb, Utils.getLogger(SaveExcludeFilesList.class));
	}
}
