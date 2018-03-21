import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import sam.backup.manager.file.FileTree;

public class MainTest {

	public static void main(String[] args) throws IOException {
			Path path = Paths.get("C:\\Users\\Sameer\\Documents\\MEGA\\eclipse_workplace\\javafx\\BackupManager\\compiled\\app_data\\trees");
			File[] files  = path.toFile().listFiles();
			Files.createDirectories(path.resolveSibling("tree-2/"));
			
			for (File file : files) {
				FileTree f = FileTree.read(file.toPath());
				f.write(path.resolveSibling("tree-2/"+file.getName()));
			}
			
			System.out.println("DONE");
	}

}
