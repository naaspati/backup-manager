import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import sam.backup.manager.extra.TreeType;
import sam.backup.manager.extra.Utils;
import sam.backup.manager.file.FileTreeReader;
import sam.backup.manager.file.FileTreeWalker;
import sam.backup.manager.file.db.Dir;
import sam.backup.manager.file.db.FileEntity;
import sam.backup.manager.file.db.FileTree;
import sam.io.serilizers.StringReader2;
import sam.sql.sqlite.SQLiteDB;

public class Converter {
	Path treedir = Utils.APP_DATA.resolve("trees");
	Path treedirdbs = Utils.APP_DATA.resolve("trees.dbs");

	public Converter() throws IOException, SQLException {
		String sql = StringReader2.getText(StringReader2.reader().source(ClassLoader.getSystemResourceAsStream("sql.sql")));
		
		List<String> map = new ArrayList<>();
		map.add("db_name\towner");
		
		Files.createDirectories(treedirdbs);
		
		for (String s : treedir.toFile().list()) {
			process(s, map, sql);
		}
		Files.write(treedirdbs.resolve("index.tsv"), map, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		System.out.println("DONE");
	}

	private void process(String name, List<String> map, String sql) throws SQLException, IOException {
		Path p = treedir.resolve(name);
		if(Files.isHidden(p))
			return;
		
		IdentityHashMap<Dir, Dir> impl = new IdentityHashMap<>();
		IdentityHashMap<Dir, Dir> holder = new IdentityHashMap<>();
		
		name = name.substring(0, name.length() - 9).concat(".db");
		
		Path db = treedirdbs.resolve(name);
		Files.deleteIfExists(db);
		try(SQLiteDB dao = new SQLiteDB(db, true);) {
			dao.executeUpdate(sql);
			
			FileTree tree = new FileTreeReader().read(p, null);
			map.add(name+"\t"+tree.getFileName().toString().replace('\\', '/'));
			System.out.println("---------------------------------------");
			System.out.println(tree);
			
			FileTree root = new FileTree(dao, TreeType.BACKUP, tree);

			ArrayList<FileEntity> files = new ArrayList<>();
			ArrayList<Dir> dirs = new ArrayList<>();
			
			tree.walk(new FileTreeWalker() {
				Dir current;
				Dir currentD;
				
				void parent(FileEntity ft) {
					if(ft.getParent() == tree) {
						current = root;
						currentD = tree;						
					}
					if(currentD != ft.getParent()) {
						currentD = ft.getParent();
						current = impl.get(currentD);
						if(current == null)  {
							current = holder.get(currentD);
							impl.put(currentD, current);
						}
					}
				}
				@Override
				public FileVisitResult file(FileEntity ft) {
					parent(ft);
					files.add(root.file(current, ft));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult dir(Dir ft) {
					parent(ft);
					Dir d = root.dir(current, ft);
					holder.put(ft, d);
					dirs.add(d);
					return FileVisitResult.CONTINUE;
				}
			});
			
			root.save();
			System.out.println("created: "+name);
		}
	}
}
