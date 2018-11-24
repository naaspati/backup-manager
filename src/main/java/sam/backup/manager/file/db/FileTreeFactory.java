package sam.backup.manager.file.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.slf4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;

public class FileTreeFactory {
	private static volatile FileTreeFactory INSTANCE;
	private static final Logger LOGGER = Utils.getLogger(FileTreeFactory.class);

	public static FileTreeFactory getInstance() {
		if (INSTANCE != null)
			return INSTANCE;

		synchronized (FileTreeFactory.class) {
			if (INSTANCE != null)
				return INSTANCE;

			try {
				INSTANCE = new FileTreeFactory();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return INSTANCE;
		}
	}

	private final Path root = Utils.APP_DATA.resolve("trees.dbs");

	private FileTreeFactory() throws IOException {
		Files.createDirectories(root);
	}
	private Path getPath(Config config, TreeType treeType) {
		return root.resolve(treeType+"-"+config.getName().replaceAll("\\s+", "_")+".db");
	}
	private final HashMap<Path, Serializer> serializers = new HashMap<>(); 
	public FileTree newFileTree(Config c, TreeType type, boolean createNewIfNotExists) throws Exception {
		Path path = getPath(c, type);

		try {
			Serializer s = serializers.get(path);
			if(s != null)
				return s.getFileTree();
			
			if (Files.exists(path) && createNewIfNotExists) {
				if(Files.notExists(path))
					LOGGER.warn("file not found: {}", path);
				
				Serializer serializer = new DbSerializer(path, type, c.getSource(), c.getTarget());
				serializers.put(path, serializer);
				return serializer.getFileTree();
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage()+" (path: "+path+")", e);
		}
		return null;
	}


}
