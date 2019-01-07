package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.slf4j.Logger;

import sam.backup.manager.config.Config;
import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.db.DbSerializer;

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
	
	private static final HashMap<Path, Serializer<?, ?>> SERIALIZERS = new HashMap<>();
	
	/**
	 * static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			synchronized (SERIALIZERS) {
				SERIALIZERS.forEach((s,t) -> {
					try {
						if(t instanceof AutoCloseable)
							((AutoCloseable)t).close();
						else if(t instanceof Closeable)
							((Closeable)t).close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}	
		}));
	}
	 */
	

	private final Path root = Utils.APP_DATA.resolve("trees.dbs");

	private FileTreeFactory() throws IOException {
		Files.createDirectories(root);
	}
	private Path getPath(Config config, TreeType treeType) {
		return root.resolve(treeType+"-"+config.getName().replaceAll("\\s+", "_")+".db");
	}
	@SuppressWarnings("rawtypes")
	public FileTree newFileTree(Config c, TreeType type, boolean createNewIfNotExists) throws Exception {
		Path path = getPath(c, type);

		synchronized (SERIALIZERS) {
			try {
				Serializer s = SERIALIZERS.get(path);
				if(s != null)
					return s.getFileTree();
				
				if (Files.exists(path) && createNewIfNotExists) {
					if(Files.notExists(path))
						LOGGER.warn("file not found: {}", path);
					
					Serializer serializer = new DbSerializer(path, type, c.getSource(), c.getTarget());
					SERIALIZERS.put(path, serializer);
					return serializer.getFileTree();
				}
			} catch (Exception e) {
				throw new Exception(e.getMessage()+" (path: "+path+")", e);
			}
			return null;	
		}
}
}
