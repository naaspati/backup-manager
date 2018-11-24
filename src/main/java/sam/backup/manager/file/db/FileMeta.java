package sam.backup.manager.file.db;



interface FileMeta {
    String FILES_TABLE_NAME = "Files";
    String DIRS_TABLE_NAME = "Dirs";

    String ID = "_id";    // _id integer not null primary key autoincrement
    String PARENT_ID = "parent_id";    // dir_id  integer not null references Dirs(_id)
    String FILENAME = "filename";    // filename text not null
    String SRC_ATTR = AttributeMeta.SRC_ATTR;    // src_attr integer not null
    String BACKUP_ATTR = AttributeMeta.BACKUP_ATTR;    // backup_attr integer not null
}