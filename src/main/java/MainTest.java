import java.io.IOException;
import java.nio.file.Paths;

import sam.backup.manager.file.FileTree;

public class MainTest  {
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		System.out.println(FileTree.read(Paths.get("compiled/app_data/trees/_anime-500188150.filetree")).toTreeString());
	}

}
