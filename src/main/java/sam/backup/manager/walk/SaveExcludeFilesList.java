package sam.backup.manager.walk;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.apache.logging.log4j.LogManager;

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

		Utils.writeInTempDir("excluded-files/"+initialWalkMode, config.getSource(), ".txt", sb, LogManager.getLogger(SaveExcludeFilesList.class));
	}
}
