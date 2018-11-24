package sam.backup.manager.file.db;

interface DBColumns {
    String FILES_TABLE_NAME = "Files";

    String ID = "_id";    // _id integer not null primary key autoincrement
    String PARENT_ID = "parent_id";    // dir_id  integer not null references Dirs(_id)
    String FILENAME = "filename";    // filename text not null
    String SRC_ATTR = "src_attr"; 
    String BACKUP_ATTR = "backup_attr";
    
    String DIRS_TABLE_NAME = "Dirs";
    String CHILD_COUNT = "_count"; 
    
	String ATTR_TABLE_NAME = "Attributes";
	 // String ID = "_id";    // _id integer not null primary key autoincrement
 	String LASTMODIFIED = "lastModified";    // lastModified integer not null default 0
 	String SIZE = "_size";    // _size integer not null default 0

    
    String ROOT_META_TABLE_NAME = "RootMeta";

    String NAME = "name";    // name text not null
    String VALUE = "_value";    // _value text not null
}