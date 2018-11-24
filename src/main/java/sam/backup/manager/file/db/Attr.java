package sam.backup.manager.file.db;

import static sam.backup.manager.file.db.AttributeMeta.ATTR_TABLE_NAME;
import static sam.backup.manager.file.db.AttributeMeta.ID;
import static sam.backup.manager.file.db.AttributeMeta.LASTMODIFIED;
import static sam.backup.manager.file.db.AttributeMeta.SIZE;

import java.sql.SQLException;
import java.util.Collection;


public class Attr {
	final int id;
	public final long lastModified;
	public final long size;
	
	Attr(int id, long lastModified, long size){
		this.id = id;
		this.lastModified = lastModified;
		this.size = size;
	}
	public Attr(long lastModified, long size){
		this.id = -1;
		this.lastModified = lastModified;
		this.size = size;
	}
	
	Attr(int id, Attr from) {
		this.id = id;
		this.lastModified = from.lastModified;
		this.size = from.size;
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
		Attr other = (Attr) obj;
		return id == other.id;
	}
	
	@Override
	public String toString() {
		return "Attr [id=" + id + ", lastModified=" + lastModified + ", size=" + size + "]";
	}
	private static final StringBuilder INSERT_SQL = new StringBuilder().append("INSERT INTO ").append(ATTR_TABLE_NAME).append("(").append(ID).append(',').append(LASTMODIFIED).append(',').append(SIZE).append(") VALUES(");

	static final void insert(Collection<Attr> data, StringBuilder massUpdate) throws SQLException {
		if(data.isEmpty())
			return;
		
		for (Attr item: data) {
			massUpdate.append(INSERT_SQL)
			.append(item.id).append(',')
			.append(item.lastModified).append(',')
			.append(item.size).append(");\n");
		}


	}
}
 
