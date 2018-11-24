package sam.backup.manager.file.db;



interface AttributeMeta {
	String ATTR_TABLE_NAME = "Attributes";

	String ID = "_id";    // _id integer not null primary key autoincrement
	String LASTMODIFIED = "lastModified";    // lastModified integer not null default 0
	String SIZE = "_size";    // _size integer not null default 0
	
    String SRC_ATTR = "src_attr"; 
    String BACKUP_ATTR = "backup_attr"; 
}