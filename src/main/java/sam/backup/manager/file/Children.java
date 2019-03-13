package sam.backup.manager.file;

interface Children extends Iterable<FileImpl> {
	int size();
	int mod();
}
