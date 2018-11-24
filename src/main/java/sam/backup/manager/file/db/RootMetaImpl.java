package sam.backup.manager.file.db;

import static sam.backup.manager.file.db.RootMeta.NAME;
import static sam.backup.manager.file.db.RootMeta.TABLE_NAME;
import static sam.backup.manager.file.db.RootMeta.VALUE;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import sam.sql.sqlite.SQLiteDB;

class RootMetaImpl {
	private final String name;
	private final String value;
	
	public RootMetaImpl(String name, String value){
		this.name = name;
		this.value = value;
	}
	
	private static final String SELECT_ALL_SQL = "SELECT * FROM "+TABLE_NAME;
	public static Map<String, String> getAll(SQLiteDB db) throws SQLException{
		return db.collectToMap(SELECT_ALL_SQL, rs -> rs.getString(NAME), rs -> rs.getString(VALUE));
	}

	private static final String INSERT_SQL = "INSERT INTO " + TABLE_NAME+"("+String.join(",", NAME,VALUE)+") VALUES(?,?)";

	public static final int insert(RootMetaImpl[] data, SQLiteDB db) throws SQLException {
		db.executeUpdate("DELETE FROM "+TABLE_NAME);

		try(PreparedStatement p = db.prepareStatement(INSERT_SQL)) {
			for (RootMetaImpl item: data){
				p.setString(1,item.name);
				p.setString(2,item.value);
				p.addBatch();
			}
			return p.executeBatch().length;
		}
	}
}

