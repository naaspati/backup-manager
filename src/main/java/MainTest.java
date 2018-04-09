import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import sam.backup.manager.file.Attrs;
import sam.backup.manager.file.FileTree;
import sam.backup.manager.file.FileTreeEntity;
import sam.backup.manager.file.FileTreeString;
import sam.backup.manager.walk.WalkMode;

public class MainTest {
	public static void main(String[] args) throws IOException {
		Path path = Paths.get("D:\\Downloads\\seagate_backup");
		FileTree ft = new FileTree(path);

		ft.walkStarted(path);
		List<FileTreeEntity> list = new ArrayList<>();
		Random r = new Random();
		int[] count = {0,0};

		Files.walkFileTree(path, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if(ft.isRootPath(dir))
					ft.setAttr(new Attrs(attrs.lastModifiedTime().toMillis(), 0), WalkMode.SOURCE, dir);
				else
					ft.addDirectory(dir, new Attrs(5, 5), WalkMode.SOURCE);
				ft.setWalked(true);
				count[0]++;
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				FileTreeEntity e = ft.addFile(file, new Attrs(attrs.lastModifiedTime().toMillis(), attrs.size()), WalkMode.SOURCE);
				ft.setCopied(true);
				count[1]++;
				if(r.nextBoolean())
					list.add(e);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
		
		System.out.println(Arrays.toString(count));
		System.out.println(new FileTreeString(ft));
		System.out.println();
		System.out.println(list.size());
		
		for (FileTreeEntity f : list) 
			System.out.println(f.getSourcePath());
		System.out.println();
		System.out.println(new FileTreeString(ft, list)); 
	}
}
