package sam.backup.manager.file.db;

interface DirChildCountMeta {
	String TABLE_NAME = "DirChildCount";

	String DIR_ID = "dir_id";    // dir_id integer not null primary key references Dirs(_id) on delete cascade
	String COUNT = "_count";    // _count integer not null default 0
}
