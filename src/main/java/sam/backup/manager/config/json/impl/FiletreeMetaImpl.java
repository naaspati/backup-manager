package sam.backup.manager.config.json.impl;

import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.config.json.impl.JsonConfigManager.JConfig;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.nopkg.Junk;

class FiletreeMetaImpl implements FileTreeMeta {
	FileTree filetree;
	final PathWrap source, target;
	JConfig config;

	public FiletreeMetaImpl(PathWrap source, PathWrap target) {
		this.source = source;
		this.target = target;
	}

	@Override
	public PathWrap getSource() {
		return source;
	}
	@Override
	public PathWrap getTarget() {
		return target;
	}
	@Override
	public FileTree getFileTree() {
		return filetree;
	}
	@Override
	public long getLastModified() {
		return Junk.notYetImplemented(); //FIXME
	}
	@Override
	public FileTree loadFiletree(FileTreeManager manager, boolean createNewIfNotExists) throws IOException {
		return Junk.notYetImplemented(); //FIXME return this.filetree = manager.read(config, config.getType() == ConfigType.LIST ? listPath : backupPath, createNewIfNotExists);
	}
}