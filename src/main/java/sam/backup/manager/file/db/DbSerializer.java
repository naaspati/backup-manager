package sam.backup.manager.file.db;


import static sam.backup.manager.file.db.DBColumns.ATTR_TABLE_NAME;
import static sam.backup.manager.file.db.DBColumns.BACKUP_ATTR;
import static sam.backup.manager.file.db.DBColumns.CHILD_COUNT;
import static sam.backup.manager.file.db.DBColumns.DIRS_TABLE_NAME;
import static sam.backup.manager.file.db.DBColumns.FILENAME;
import static sam.backup.manager.file.db.DBColumns.FILES_TABLE_NAME;
import static sam.backup.manager.file.db.DBColumns.ID;
import static sam.backup.manager.file.db.DBColumns.LASTMODIFIED;
import static sam.backup.manager.file.db.DBColumns.NAME;
import static sam.backup.manager.file.db.DBColumns.PARENT_ID;
import static sam.backup.manager.file.db.DBColumns.ROOT_META_TABLE_NAME;
import static sam.backup.manager.file.db.DBColumns.SIZE;
import static sam.backup.manager.file.db.DBColumns.SRC_ATTR;
import static sam.backup.manager.file.db.DBColumns.VALUE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import sam.backup.manager.extra.TreeType;
import sam.collection.Iterable2;
import sam.io.serilizers.StringReader2;
import sam.myutils.Checker;
import sam.sql.JDBCHelper;
import sam.sql.SqlConsumer;
import sam.sql.sqlite.SQLiteDB;
import sam.string.BasicFormat;

public class DbSerializer implements Serializer {
	private DbDir[] dirs;
	private DbFileImpl[] files;
	private DbAttr[] attrs;
	
	private int dirIdMax;
	private int fileIdMax; 
	private int attrIdMax;

	private static final String TREE_TYPE = "TREE_TYPE";
	private static final String SAVED_ON = "SAVED_ON";
	private static final String PATH = "PATH";
	private static final String BACKUP_PATH = "BACKUP_PATH";

	private static String INIT_SQL;

	public static final DbAttr DEFAULT_ATTR = new DbAttr(0, 0, 0);
	@Override
	public Attrs defaultAttrs() {
		return new Attrs(DEFAULT_ATTR);
	}
	private final Path dbpath;
	private FileTreeImpl fileTree;
	private SQLiteDB db;

	public DbSerializer(Path dbpath, TreeType type, Path sourceDirPath, Path backupDirPath) {
		this.dbpath = dbpath;

		if(Files.exists(dbpath)) 
			this.fileTree = read(dbpath, type, sourceDirPath, backupDirPath);
		else {
			dirs = new DbDir[0];
			files = new DbFileImpl[0];
			new FileTreeImpl(this, type, sourceDirPath, backupDirPath);
		}
	}

	private class TempDirDb {
		final int id, child_count, parent_id;
		final String filename;
		final Attrs source, backup;

		public TempDirDb(int id, String name, int parent_id, Attrs src, Attrs backup, int child_count) {
			this.id = id;
			this.parent_id = parent_id;
			this.filename = name;
			this.source = src;
			this.backup = backup;
			this.child_count = child_count;
		}

		public DbDir map(Dir parent) {
			return new DbDir(fileTree, id, child_count, parent, filename, source, backup);
		}

		@Override
		public String toString() {
			return "TempDirDb [id=" + id + ", child_count=" + child_count + ", parent_id=" + parent_id + ", filename="
					+ filename + ", source=" + source + ", backup=" + backup + "]";
		}

	}

	private FileTreeImpl read(Path dbpath, TreeType type, Path sourceDirPath, Path backupDirPath) {
		db.executeUpdate(INIT_SQL);
		StringBuilder sb = new StringBuilder();
		db.executeUpdate(sb.toString());
		db.commit();

		Attrs[] filetreeAttrs = {null, null};

		try {
			Map<String, String> meta = db.collectToMap("SELECT * FROM".concat(ROOT_META_TABLE_NAME), rs -> rs.getString(NAME), rs -> rs.getString(VALUE));

			String s = meta.get(TREE_TYPE);
			if(s == null || TreeType.valueOf(s) != type)
				throw new SQLException("RootMeta Error: Different TreeType: "+s+" != "+type);
			s = meta.get(PATH);
			if(s == null || !Paths.get(s).equals(sourceDirPath))
				throw new SQLException("RootMeta Error: Different SourcePath: "+s+" != "+sourceDirPath);

			attrs = new DbAttr[Integer.parseInt(meta.get(ATTR_TABLE_NAME))];
			dirs = new DbDir[Integer.parseInt(meta.get(DIRS_TABLE_NAME))];
			files = new DbFileImpl[Integer.parseInt(meta.get(FILES_TABLE_NAME))];
			attrs[0] = DEFAULT_ATTR;

			getAll(ATTR_TABLE_NAME, rs -> {
				DbAttr a = new DbAttr(rs);
				attrs[a.id] = a; 
			});

			HashMap<Integer, List<TempDirDb>> no_parent = new HashMap<>();

			getAll(DIRS_TABLE_NAME, rs -> {
				Attrs src = new Attrs(attrs[rs.getInt(SRC_ATTR)]);
				Attrs backup = new Attrs(attrs[rs.getInt(BACKUP_ATTR)]);

				int parent_id = rs.getInt(PARENT_ID);
				int id = rs.getInt(ID);
				String name = rs.getString(FILENAME);
				int child_count = rs.getInt(CHILD_COUNT);

				// expected fileTree to be null
				Dir parent = parent_id < 0 ? fileTree : dirs[parent_id];
				Dir item = null;

				if(id == 0) {
					item = fileTree = new FileTreeImpl(this, type, sourceDirPath, backupDirPath, src, backup, child_count);
				} else {
					if(parent == null)
						no_parent.computeIfAbsent(parent_id, i -> new ArrayList<>()).add(new TempDirDb(id, name, parent_id, src, backup, child_count));
					else {
						DbDir d = new DbDir(fileTree, id, child_count, parent, name, src, backup);
						item = d;
						parent.addChild(d);
						this.dirs[d.id] = d;
					}
				}
				if(item != null) {
					List<TempDirDb> list = no_parent.remove(id);
					if(list != null) 
						list.forEach(d -> item.addChild(d.map(item)));
				}
			});

			if(!no_parent.isEmpty()) {
				throw new IllegalStateException("no_parent must be empty at this point: " + no_parent);
			}


			getAll(FILES_TABLE_NAME, rs -> {
				Attrs src = new Attrs(attrs[rs.getInt(SRC_ATTR)]);
				Attrs backup = new Attrs(attrs[rs.getInt(BACKUP_ATTR)]);

				int id = rs.getInt(ID);
				String name = rs.getString(FILENAME); 

				DbDir parent = dirs[rs.getInt(PARENT_ID)]; 
				DbFileImpl f = new DbFileImpl(id, parent, name, src, backup);
				parent.addChild(f);
				files[f.id] = f; 
			});
		} catch (SQLException e) {
			db.close();
			throw e;
		}
	}
	public void save() {
		StringBuilder batchSQL = new StringBuilder();
		
		//TODO

		if(db == null) {
			db = new SQLiteDB(dbpath, true);

			if(INIT_SQL == null)
				INIT_SQL = StringReader2.reader().source(ClassLoader.getSystemResourceAsStream("sql.sql")).read();

			db.executeUpdate(INIT_SQL);

			insertAttrs(Collections.singletonList(DEFAULT_ATTR), batchSQL);
		}

		ArrayList<Attr> newAttrs = new ArrayList<>(dirs.size()+files.size());
		Consumer<Attrs> setId = attrs -> {
			if(id(attrs) == -1) {
				Attr c = attrs.current();
				if(c.lastModified == 0 && c.size == 0)
					attrs.setCurrent(DEFAULT_ATTR);
				else {
					Attr a = new Attr(attrSerial++, c);
					newAttrs.add(a);
				}
			}
		};
		for (DbDir d : dirs) {
			setId.accept(d.getSourceAttrs());
			setId.accept(d.getBackupAttrs());
		}
		for (DbFileImpl d : files) {
			setId.accept(d.getSourceAttrs());
			setId.accept(d.getBackupAttrs());
		}

		insertAttrs(newAttrs, batchSQL);
		insertDirs(dirSerial, dirs.iterableOfCurrent(), db);
		insertFiles(fileSerial, files.iterableOfCurrent(), db);

		updateDirChildCounts(batchSQL);
		updateAttributes(batchSQL);

		batchSQL.append("DELETE FROM ").append(ROOT_META_TABLE_NAME).append(";\n");
		BasicFormat format = new BasicFormat(JDBCHelper.insertSQL(ROOT_META_TABLE_NAME, NAME, VALUE).replace("?", "'{}'"));

		format.format(batchSQL, DIRS_TABLE_NAME, dirIdMax);
		format.format(batchSQL, FILES_TABLE_NAME, fileIdMax);
		format.format(batchSQL, ATTR_TABLE_NAME, attrIdMax);
		if(fileTree.getSourcePath() != null)
			format.format(batchSQL, PATH, fileTree.getSourcePath().toString().replace("'", "''"));
		if(fileTree.getBackupPath() != null)
			format.format(batchSQL, BACKUP_PATH, fileTree.getBackupPath().toString().replace("'", "''"));
		if(fileTree.getTreetype() != null)
			format.format(batchSQL, TREE_TYPE, fileTree.getTreetype());

		format.format(batchSQL, SAVED_ON, LocalDateTime.now().toString().replace("'", "''"));

		db.executeUpdate(batchSQL.toString());
		db.commit();
	}

	static int id(FileImpl item, Attrs a) {
		int id = ((DbAttr)a.current()).id;

		if(id < 0)
			throw new IllegalArgumentException("Attrs not persisted yet: "+item+"  "+a);
		return id;
	}

	private void getAll(String tableName, SqlConsumer<ResultSet> consumer) throws SQLException {
		db.iterate("SELECT * FROM "+tableName, consumer);
	}
	/* TODO 
	public static final int insertDirs(Iterable<Dir> data, SQLiteDB db) throws SQLException {
		return insert(DBColumns.DIRS_TABLE_NAME, data, db, d -> !d.isDirectory(), "Not a Dir: ");
	}
	private void updateAttributes(StringBuilder sb) {
		String format = BasicFormat.format("UPDATE {} SET {}=\\{\\}, {}=\\{\\} WHERE {}=\\{\\};\n", FILES_TABLE_NAME, SRC_ATTR, BACKUP_ATTR, ID);
		update(sb, dirSerial, new BasicFormat(format), dirs);
		update(sb, fileSerial, new BasicFormat(format.replaceFirst(FILES_TABLE_NAME, DIRS_TABLE_NAME)), files);
	}
	private static <E extends FileImpl> void update(StringBuilder sb, BasicFormat update) {
		for (FileImpl d : list.iterableOfCurrent()) {
			update(sb, update, d, d.getSourceAttrs(), d.getBackupAttrs());
		}

		for (FileImpl d : list.fixed) {
			if(d != null)
				update(sb, update, d, d.getSourceAttrs(), d.getBackupAttrs());
		}
	}
	private static void update(StringBuilder sb, BasicFormat update, FileImpl d, Attrs src, Attrs backup) {
		if(!(src.isModified() || backup.isModified()))
			return ;
		update.format(sb, id(d, src), id(d, backup));
	}
	private void updateDirChildCounts(StringBuilder sb) {
		BasicFormat insert = new BasicFormat(BasicFormat.format("INSERT INTO {}({},{}) VALUES(\\{\\},\\{\\});\n", DirChildCountMeta.TABLE_NAME, DirChildCountMeta.DIR_ID, DirChildCountMeta.COUNT));
		BasicFormat update = new BasicFormat(BasicFormat.format("UPDATE {} SET {}=\\{\\} WHERE {}=\\{\\};\n", DirChildCountMeta.TABLE_NAME, DirChildCountMeta.COUNT, DirChildCountMeta.DIR_ID));

		for (Dir d : dirs.iterableOfCurrent()) {
			if(dirSerial.isNew(d.id))
				insert.format(sb, d.id, d.childrenCount());
			else
				update.format(sb, d.childrenCount(), d.id);	
		}

		for (Dir d : dirs.fixed) {
			if(d != null && d.childrenCount() != childCount[d.id])
				update.format(sb, d.childrenCount(), d.id);
		}
	}

	public static final int insertFiles(Iterable<FileImpl> data, SQLiteDB db) throws SQLException {
		return insert(FILES_TABLE_NAME, serial, data, db, f -> f.isDirectory(), "trying to insert a Dir as file: ");
	}
	protected static int insertFiles(Iterable<? extends FileImpl> data, SQLiteDB db, Predicate<FileImpl> check, String msg) throws SQLException {
		Iterable2<? extends FileImpl> list = Iterable2.wrap(data);
		if(!list.hasNext()) return 0;

		String insertSQL = "INSERT INTO " + tableName +"("+String.join(",", ID,PARENT_ID,FILENAME,SRC_ATTR,BACKUP_ATTR)+") VALUES(?,?,?,?,?)";

		try(PreparedStatement p = db.prepareStatement(insertSQL)) {
			for (FileImpl item: list){
				if(serial.isNew(item.id)) {
					if(check.test(item))
						throw new IllegalArgumentException(msg+item);

					p.setInt(1,item.id);
					p.setInt(2,item.getParentId());
					p.setString(3,item.filename);
					p.setInt(4,id(item, item.getSourceAttrs()));
					p.setInt(5,id(item, item.getBackupAttrs()));
					p.addBatch();
				}
			}
			return p.executeBatch().length;
		}
	}
	*/

	@Override
	public FileImpl newFile(Dir parent, String filename) {
		return new DbFileImpl(fileIdMax, parent, filename, defaultAttrs(), defaultAttrs());
	}
	@Override
	public FileImpl newDir(Dir parent, String filename) {
		return new DbDir(fileTree, dirIdMax, 0, parent, filename, defaultAttrs(), defaultAttrs());
	}
	@Override
	public void applyToAll(Consumer<FileImpl> action) {
		//FIXME
		throw new IllegalAccessError();
	}
	@Override
	public FileTreeImpl getFileTree() {
		return fileTree;
	}

	private static final String ATTRS_INSERT_SQL = Optional.of(JDBCHelper.insertSQL(ATTR_TABLE_NAME, ID, LASTMODIFIED, SIZE)).map(s -> s.substring(0, s.lastIndexOf('(')+1)).get(); 

	static final void insertAttrs(Collection<DbAttr> data, StringBuilder sink) throws SQLException {
		if(data.isEmpty())
			return;

		for (DbAttr item: data) {
			sink.append(ATTRS_INSERT_SQL)
			.append(item.id).append(',')
			.append(item.lastModified).append(',')
			.append(item.size).append(");\n");
		}
	}

	private class DbFileImpl extends FileImpl {
		final int id;

		DbFileImpl(int id, Dir parent, String filename, Attrs source, Attrs backup) {
			super(parent, filename, source, backup);
			this.id = id;
		}
		@Override
		public final int hashCode() {
			return id;
		}
		@Override
		public final boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;

			DbFileImpl other = (DbFileImpl) obj;
			if (id != other.id)
				return false;
			if(other != this) throw new IllegalArgumentException("two different Entity have same id: "+this+"  "+other);
			return true;
		}
		@Override
		public String toString() {
			return getClass().getSimpleName()+" [id=" + id + ", filename=" + filename + ", srcAttrs=" + srcAttrs + ", backupAttrs="
					+ backupAttrs + "]";
		}
	}

	private class DbDir extends Dir {
		public final int id;
		public final int child_count;

		DbDir(FileTreeImpl root, int id, int child_count, Dir parent, String filename, Attrs source, Attrs backup){
			super(root, parent, filename, source, backup, new ArrayList<>(child_count+1));
			this.id = id;
			this.child_count  = child_count;
		}
		@Override
		public String toString() {
			return getClass().getSimpleName()+" [id=" + id + ", filename=" + filename + ", srcAttrs=" + srcAttrs + ", backupAttrs="
					+ backupAttrs + "]";
		}
	}
	private class DbAttr extends Attr {

		final int id;

		DbAttr(ResultSet rs) throws SQLException {
			super(rs.getLong(LASTMODIFIED), rs.getLong(SIZE));
			this.id = rs.getInt(ID);
		}
		DbAttr(int id, long lastModified, long size){
			super(lastModified, size);
			this.id = id;
		}
		DbAttr(int id, DbAttr from) {
			super(from);
			this.id = id;
		}
		@Override
		public String toString() {
			return "DbAttr [id=" + id + ", lastModified=" + lastModified + ", size=" + size + "]";
		}

	}
}
