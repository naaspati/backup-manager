package sam.backup.manager.walk;

import static sam.backup.manager.extra.Utils.TEMPS_DIR;
import static sam.backup.manager.extra.Utils.hashedName;
import static sam.backup.manager.extra.Utils.subpath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.apache.logging.log4j.LogManager;

import sam.backup.manager.config.Config;
import sam.fileutils.FilesUtils;
import sam.fx.popup.FxPopupShop;

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

		Path p = TEMPS_DIR.resolve("excluded-files/"+initialWalkMode).resolve(hashedName(config.getSource(), ".txt"));
		try {
			Files.createDirectories(p.getParent());
			FilesUtils.appendFileAtTop(sb.toString().getBytes(), p);
			LogManager.getLogger(getClass()).info("created: {}", subpath(p, TEMPS_DIR));
		} catch (IOException e) {
			LogManager.getLogger(getClass()).error("error occured while saving: "+p, e);
			FxPopupShop.showHidePopup("error occured", 1500);
		}
	}
}
