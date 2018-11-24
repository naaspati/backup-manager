package sam.backup.manager.file.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;

public class RootFactory {
	private static volatile RootFactory INSTANCE;
	private static final Logger LOGGER = Utils.getLogger(RootFactory.class);

	public static RootFactory getInstance() {
		if (INSTANCE != null)
			return INSTANCE;

		synchronized (RootFactory.class) {
			if (INSTANCE != null)
				return INSTANCE;

			try {
				INSTANCE = new RootFactory();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return INSTANCE;
		}
	}

	private final Path root = Utils.APP_DATA.resolve("trees.dbs");

	private RootFactory() throws IOException {
		Files.createDirectories(root);
	}
	private Path getDbPath(Config config, TreeType treeType) {
		return root.resolve(treeType+"-"+config.getName().replaceAll("\\s+", "_")+".db");
	}
	public FileTree newFileTree(Config c, TreeType type, boolean createNewIfNotExists) throws Exception {
		Path dbpath = getDbPath(c, type);

		try {
			if (Files.exists(dbpath))
				return new DbSerializer(type, dbpath, c.getSource(), c.getTarget()).read();
			else {
				LOGGER.warn("dbfile not found: {}", dbpath);
				if(createNewIfNotExists)
					return new FileTree(type, c.getSource(), c.getTarget());
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage()+" (dbpath: "+dbpath+")", e);
		}
		return null;
	}


}
