package sam.backup.manager.file.db;

import static sam.backup.manager.file.db.AttributeMeta.ATTR_TABLE_NAME;
import static sam.backup.manager.file.db.AttributeMeta.ID;
import static sam.backup.manager.file.db.AttributeMeta.LASTMODIFIED;
import static sam.backup.manager.file.db.AttributeMeta.SIZE;
import static sam.backup.manager.file.db.FileMeta.BACKUP_ATTR;
import static sam.backup.manager.file.db.FileMeta.DIRS_TABLE_NAME;
import static sam.backup.manager.file.db.FileMeta.FILENAME;
import static sam.backup.manager.file.db.FileMeta.FILES_TABLE_NAME;
import static sam.backup.manager.file.db.FileMeta.ID;
import static sam.backup.manager.file.db.FileMeta.PARENT_ID;
import static sam.backup.manager.file.db.FileMeta.SRC_ATTR;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import sam.backup.manager.extra.TreeType;
import sam.backup.manager.file.db.FileTree.Serial;
import sam.collection.Iterable2;
import sam.io.serilizers.StringReader2;
import sam.sql.SqlConsumer;
import sam.sql.sqlite.SQLiteDB;
import sam.string.BasicFormat;

public class DbSerializer implements Serializer {
	private Serial dirSerial;
	private Serial fileSerial;
	private int attrSerial;

	private final SQLiteDB db;

	private static final Dir[] DIRS_DEFAULT  = new Dir[0];
	private static final FileImpl[] FILES_DEFAULT  = new FileImpl[0];

	private final List2<Dir> dirs;
	private final List2<FileImpl> files;
	private final int[] childCount;

	private static final String TREE_TYPE = "TREE_TYPE";
	private static final String SAVED_ON = "SAVED_ON";
	private static final String PATH = "PATH";
	private static final String BACKUP_PATH = "BACKUP_PATH";

	private static String INIT_SQL;

	public DbSerializer(Path dbpath) {
		if(Files.exists(dbpath))
			throw new IllegalStateException("DB already exist: "+dbpath);

		this.db = new SQLiteDB(dbpath, true);

		if(INIT_SQL == null)
			INIT_SQL = StringReader2.reader().source(ClassLoader.getSystemResourceAsStream("sql.sql")).read();

		db.executeUpdate(INIT_SQL);
		StringBuilder sb = new StringBuilder();
		Attr.insert(Collections.singletonList(DEFAULT_ATTR), sb);
		db.executeUpdate(sb.toString());
		db.commit();

		this.backupPath = backupDirPath;
		this.srcPath = sourceDirPath;
		dirSerial = new Serial();
		fileSerial = new Serial(); 
		attrSerial = 1;

		this.dirs = new List2<>(DIRS_DEFAULT, Dir[]::new);
		this.files = new List2<>(FILES_DEFAULT, FileImpl[]::new);
		this.childCount = new int[0];

		dirs.set(0, this);
	}

	DbSerializer(TreeType type, Path dbpath, Path sourceDirPath, Path backupDirPath) throws SQLException {
		super(null, 0, null, sourceDirPath.toString(), defaultAttrs(), defaultAttrs());
		this.db = new SQLiteDB(dbpath);

		try {
			Map<String, String> meta = RootMetaImpl.getAll(db);
			String s = meta.get(TREE_TYPE);
			if(s == null || TreeType.valueOf(s) != treetype)
				throw new SQLException("RootMeta Error: Different TreeType: "+s+" != "+treetype);
			s = meta.get(PATH);
			if(s == null || !Paths.get(s).equals(sourceDirPath))
				throw new SQLException("RootMeta Error: Different SourcePath: "+s+" != "+sourceDirPath);

			dirSerial = serial(meta.get(DIRS_TABLE_NAME));
			fileSerial = serial(meta.get(FILES_TABLE_NAME)); 
			attrSerial = Integer.parseInt(meta.get(ATTR_TABLE_NAME));

			Attr[] attrs = new Attr[attrSerial];

			getAll(ATTR_TABLE_NAME, rs -> {
				Attr a = new Attr(rs);
				attrs[a.id] = a; 
			});

			attrs[0] = new Attr(0, 0, 0);

			childCount = new int[dirSerial.current];
			getAll(DirChildCountMeta.TABLE_NAME, rs -> childCount[rs.getInt(DirChildCountMeta.DIR_ID)] = rs.getInt(DirChildCountMeta.COUNT));

			this.dirs = new List2<>(new Dir[dirSerial.current], Dir[]::new);

			getAll(DIRS_TABLE_NAME, rs -> {
				Attrs src = new Attrs(attrs[rs.getInt(SRC_ATTR)]);
				Attrs backup = new Attrs(attrs[rs.getInt(BACKUP_ATTR)]);

				int parent_id = rs.getInt(PARENT_ID);
				int id = rs.getInt(ID);
				String name = rs.getString(FILENAME); 

				Dir d;
				if(parent_id < 0) {
					d = this;
					this.srcAttrs = src;
					this.backupAttrs = backup;

				} else {
					Dir parent = dirs.fixed[parent_id]; 
					d = new Dir(this, id, parent, name, src, backup);
					parent.addChild(d);
				}
				this.dirs.fixed[d.id] = d; 
			});

			this.files = new List2<>(new FileImpl[fileSerial.current], FileImpl[]::new);

			getAll(FILES_TABLE_NAME, rs -> {
				Attrs src = new Attrs(attrs[rs.getInt(SRC_ATTR)]);
				Attrs backup = new Attrs(attrs[rs.getInt(BACKUP_ATTR)]);

				int id = rs.getInt(ID);
				String name = rs.getString(FILENAME); 

				Dir parent = dirs.fixed[rs.getInt(PARENT_ID)]; 
				FileImpl f = new FileImpl(id, parent, name, src, backup);
				parent.addChild(f);
				files.fixed[f.id] = f; 
			});

		} catch (SQLException e) {
			db.close();
			throw e;
		}
	}

	private void getAll(String tableName, SqlConsumer<ResultSet> consumer) throws SQLException {
		db.iterate("SELECT * FROM "+tableName, consumer);
	}
	private Serial serial(String s) {
		return s == null ? new Serial() : new Serial(Integer.parseInt(s));
	}
	public static final int insertDirs(Serial serial, Iterable<Dir> data, SQLiteDB db) throws SQLException {
		return insert(FileMeta.DIRS_TABLE_NAME, serial, data, db, d -> !d.isDirectory(), "Not a Dir: ");
	}
	public void save() throws SQLException {
		StringBuilder massUpdate = new StringBuilder();

		ArrayList<Attr> newAttrs = new ArrayList<>(dirs.size()+files.size());
		Consumer<Attrs> setId = attrs -> {
			if(attrs.current().id == -1) {
				Attr c = attrs.current();
				if(c.lastModified == 0 && c.size == 0)
					attrs.setCurrent(DEFAULT_ATTR);
				else {
					Attr a = new Attr(attrSerial++, c);
					newAttrs.add(a);
				}
			}
		};
		dirs.forEach(d -> {
			setId.accept(d.getSourceAttrs());
			setId.accept(d.getBackupAttrs());
		});
		files.forEach(d -> {
			setId.accept(d.getSourceAttrs());
			setId.accept(d.getBackupAttrs());
		});

		Attr.insert(newAttrs, massUpdate);
		Dir.insertDirs(dirSerial, dirs.iterableOfCurrent(), db);
		FileImpl.insertFiles(fileSerial, files.iterableOfCurrent(), db);

		updateDirChildCounts(massUpdate);
		updateAttributes(massUpdate);

		if(massUpdate.length() != 0)
			db.executeUpdate(massUpdate.toString());

		RootMetaImpl[] array = {
				to(DIRS_TABLE_NAME, dirSerial),
				to(FILES_TABLE_NAME, fileSerial),
				new RootMetaImpl(ATTR_TABLE_NAME, String.valueOf(attrSerial)),
				new RootMetaImpl(PATH, getSourcePath()),
				new RootMetaImpl(BACKUP_PATH, getBackupPath()),
				new RootMetaImpl(TREE_TYPE, treetype.toString()),
				new RootMetaImpl(SAVED_ON, LocalDateTime.now().toString()),
		};
		RootMetaImpl.insert(array, db);

		db.commit();

		dirSerial = new Serial(dirSerial);
		fileSerial = new Serial(fileSerial); 
	}

	private void updateAttributes(StringBuilder sb) {
		String format = BasicFormat.format("UPDATE {} SET {}=\\{\\}, {}=\\{\\} WHERE {}=\\{\\};\n", FILES_TABLE_NAME, SRC_ATTR, BACKUP_ATTR, ID);
		update(sb, dirSerial, new BasicFormat(format), dirs);
		update(sb, fileSerial, new BasicFormat(format.replaceFirst(FILES_TABLE_NAME, DIRS_TABLE_NAME)), files);
	}
	private static <E extends FileImpl> void update(StringBuilder sb, Serial serial, BasicFormat update, List2<E> list) {
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
	private RootMetaImpl to(String name, Serial serial) {
		return new RootMetaImpl(name, String.valueOf(serial.current));
	}
	public static final int insertFiles(Serial serial, Iterable<FileImpl> data, SQLiteDB db) throws SQLException {
		return insert(FILES_TABLE_NAME, serial, data, db, f -> f.isDirectory(), "trying to insert a Dir as file: ");
	}
	protected static int insert(String tableName, Serial serial, Iterable<? extends FileImpl> data, SQLiteDB db, Predicate<FileImpl> check, String msg) throws SQLException {
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
	Attr readAttr(ResultSet rs) throws SQLException {
		new Attr(
				rs.getInt(AttributeMeta.ID), 
				rs.getInt(AttributeMeta.LASTMODIFIED), 
				rs.getInt(AttributeMeta.SIZE)
				);
	}

	public FileTree read() {
		// TODO Auto-generated method stub
		return null;
	}

}
