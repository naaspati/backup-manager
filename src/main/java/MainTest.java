import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import sam.backup.manager.file.FileTree;

public class MainTest {

	public static void main(String[] args) throws IOException {
			Path path = Paths.get("app_data\\tree");
			File[] files  = path.toFile().listFiles();
			Files.createDirectories(path.resolveSibling("tree-2"));
			
			for (File file : files) {
				System.out.println(file.getName());
				FileTree f = FileTree.read(file.toPath());
				Files.write(Paths.get("D:\\Downloads\\a.txt"), f.toTreeString().getBytes(), StandardOpenOption.CREATE);
				System.exit(0);
			}
			
			System.out.println("DONE");
	}

}
