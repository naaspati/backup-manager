import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTree;

public class MainTest implements FileVisitor<Path> {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		new MainTest();
	}

	private Predicate<Path> excluder, includer;
	private int nameCount;

	RootConfig root;
	public MainTest() throws IOException {
		Utils.APP_DATA = Paths.get("compiled/app_data");


		root = Utils.readConfigJson();
		// System.out.println(root.getSourceExcluder());
		Config c;

		index(1);
		index(0);
		index(2);


		// walk(config.getTarget(), config.getTargetExcluder(), p -> false, WalkType.BACKUP);
	}
	private void index(int i) throws IOException {
		Config c = root.getBackups()[i];
		System.out.println(c.getSourceText());
		c.setFileTree(new FileTree(c.getSource()));
		nameCount = c.getSource().getNameCount();
		excluder = c.getSourceExcluder();
		includer = c.getSourceIncluder();

		Files.walkFileTree(c.getSource(), this);

		System.out.println("\n");
	}

	//TODO  preVisitDirectory
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		int end = dir.getNameCount();

		if(end != nameCount && include(dir)) {
		}
		else if(end != nameCount) {
			System.out.println(dir);
			return FileVisitResult.SKIP_SUBTREE;
		}

		return FileVisitResult.CONTINUE;
	}

	// TODO visitFile
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(!include(file)) {
			System.out.println("  "+file);
		}

		return FileVisitResult.CONTINUE;
	}

	private boolean include(Path file) {
		return includer.test(file) || !excluder.test(file);
	}
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}
}
