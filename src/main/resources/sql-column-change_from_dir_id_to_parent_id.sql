DROP TABLE IF EXISTS RootMeta2;
DROP TABLE IF EXISTS Attributes2;
DROP TABLE IF EXISTS DirChildCount2;
DROP TABLE IF EXISTS Files2;
DROP TABLE IF EXISTS Dirs2;

create table RootMeta2 (
  name text not null,
  _value text not null
);

create table Attributes2 (
  _id integer not null primary key unique,
  lastModified integer not null,
  _size integer not null
);

create table Dirs2 ( -- copy of Files2, keeping tables seperate for more continues _id 
  _id integer not null primary key unique,
  parent_id  integer not null,
  filename text not null,
  src_attr integer not null, 
  backup_attr integer not null
);

create table DirChildCount2 (
  dir_id integer not null primary key references Dirs2(_id) on delete cascade,
  _count integer not null default 0  
);

create table Files2 (
  _id integer not null primary key unique,
  parent_id  integer not null references Dirs2(_id),
  filename text not null,
  src_attr integer not null, 
  backup_attr integer not null
);


INSERT INTO RootMeta2 SELECT * FROM RootMeta;
INSERT INTO Attributes2 SELECT * FROM Attributes;
INSERT INTO Dirs2 SELECT * FROM Dirs;
INSERT INTO DirChildCount2 SELECT * FROM DirChildCount;
INSERT INTO Files2 SELECT * FROM Files;

DROP TABLE RootMeta;
DROP TABLE Attributes;
DROP TABLE DirChildCount;
DROP TABLE Files;
DROP TABLE Dirs;


ALTER TABLE RootMeta2 RENAME TO RootMeta;
ALTER TABLE Attributes2 RENAME TO Attributes;
ALTER TABLE DirChildCount2 RENAME TO DirChildCount;
ALTER TABLE Files2 RENAME TO Files;
ALTER TABLE Dirs2 RENAME TO Dirs;
