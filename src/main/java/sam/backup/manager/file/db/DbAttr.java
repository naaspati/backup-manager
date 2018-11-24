package sam.backup.manager.file.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import static sam.backup.manager.file.db.DBColumns.*;

public class DbAttr extends Attr {
	
	public final int id;
	
	public DbAttr(ResultSet rs) throws SQLException {
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
	public int hashCode() {
		return id;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		DbAttr other = (DbAttr) obj;
		return id == other.id;
	}
	
	@Override
	public String toString() {
		return "DbAttr [id=" + id + ", lastModified=" + lastModified + ", size=" + size + "]";
	}
	
}
 
