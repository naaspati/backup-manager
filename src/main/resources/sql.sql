create table RootMeta (
  name text not null,
  _value text not null
);

create table Attributes (
  _id integer not null primary key unique,
  lastModified integer not null,
  _size integer not null
);

create table Dirs ( -- copy of Files, keeping tables seperate for more continues _id 
  _id integer not null primary key unique,
  parent_id  integer not null,
  filename text not null,
  src_attr integer not null, 
  backup_attr integer not null
  child_count integer not null
);
create table Files (
  _id integer not null primary key unique,
  parent_id  integer not null references Dirs(_id),
  filename text not null,
  src_attr integer not null, 
  backup_attr integer not null
);