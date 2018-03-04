package sam.backup.manager.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import sam.backup.manager.config.Config;
import sam.backup.manager.config.RootConfig;
import sam.backup.manager.file.FileTreeReader.Values;
import sam.backup.manager.walk.WalkType;

public class FileTree extends DirEntity {
	private static final String VERSION_STRING = "FileTreeEntity: version:1.1";
	private volatile DirLocaltor locator;
	private volatile Path currentParent;
	private volatile DirLocaltor currentLocator;

	private final class DirLocaltor {
		private final DirEntity value;
		private final Map<Path, DirLocaltor> map = new HashMap<>();

		DirLocaltor(DirEntity value) {
			this.value = value;
		}
		FileTreeEntity walk(Path subpath, boolean isDir) throws IOException {
			if(subpath.getNameCount() == 1)
				return add(subpath, isDir);
			else if(subpath.getParent().equals(currentParent))
				return currentLocator.add(subpath.getFileName(), isDir);
			else 
				return walk2(subpath, 0, isDir);
		}
		FileTreeEntity walk2(Path subpath, int index, boolean isDir) throws IOException {
			Path name = subpath.getName(index);

			if(subpath.getNameCount() == index + 1) {
				setCurrent(subpath);
				return add(name, isDir);
			}

			DirLocaltor dl = map.get(name);
			if(dl == null) {
				FileTreeEntity e = add(name, isDir);
				if(!e.isDirectory()) {
					setCurrent(subpath);
					return e;
				}
				dl = map.get(name);
			}
			if(dl == null) 
				throw new IOException(String.format("failed to complete walk: subpath:%s, index:%s, isDir:%s ", subpath, index, isDir));

			return dl.walk2(subpath, index+1, isDir);
		}
		private FileTreeEntity add(Path name, boolean isDir) throws IOException {
			FileTreeEntity entity = value.addChild(name, isDir);

			if(entity.isDirectory())
				map.put(name, new DirLocaltor(entity.castDir()));

			return entity;
		}
		public void setCurrent(Path subpath) {
			currentLocator = this;
			currentParent = subpath.getParent();
		}
	}

	public DirEntity addDirectory(Path subpath, Path fullpath, long lastModifiedTime, WalkType walkType) throws IOException {
		return add(subpath, fullpath, new AboutFile(lastModifiedTime, 0), walkType, true).castDir();
	}
	
	public FileEntity addFile(Path subpath, Path fullpath, AboutFile aboutFile, WalkType walkType) throws IOException {
		return add(subpath, fullpath, aboutFile, walkType, false).castFile();
	}
	private FileTreeEntity add(Path subpath, Path fullpath, AboutFile aboutFile, WalkType walkType, boolean isDirectory) throws IOException {
		if(locator == null) {
			locator = new DirLocaltor(this);
			currentLocator = locator;
		}
		FileTreeEntity e = locator.walk(subpath, isDirectory);
		e.setAboutFile(aboutFile, walkType, fullpath);
		return e;
	}
	@Override
	protected int cleanup() {
		Iterator<FileTreeEntity> ft = getChildren().iterator();
		int sum = 0;
		while(ft.hasNext()) {
			FileTreeEntity f = ft.next();
			if(f.getSourceAboutFile() == null) {
				ft.remove();
				sum++;
			}
			else if(f.isDirectory())
				sum += f.castDir().cleanup();
		}
		return sum;
	}
	public static FileTree read(Path path) throws IOException {
		try(InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
				DataInputStream dis = new DataInputStream(is);
				FileTreeReader reader = new FileTreeReader(dis)) {

			String version = dis.readUTF();
			if(!VERSION_STRING.equals(version))
				throw new IOException("not a filetree file");

			return new FileTree(reader, reader.next()); 
		}
	}

	public FileTree(Path path) {
		super(path);
	}
	@Override
	public Path getSourcePath() {
		return super.getFileName();
	}
	public FileTree(FileTreeReader reader, Values next) throws IOException {
		super(reader, next);
	}
	public void write(Path path) throws IOException {
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				DataOutputStream dos = new DataOutputStream(os);
				FileTreeWriter writer = new FileTreeWriter(dos)){
			dos.writeUTF(VERSION_STRING);
			writer.write(this);
		}
	}
	public String toTreeString() {
		return toTreeString(p -> true);
	}
	public String toTreeString(Predicate<FileTreeEntity> filter) {
		StringBuilder sb = new StringBuilder();
		appendDetails(sb, this, new char[0]);
		append(new char[] {' ', '|'}, sb, filter);
		return sb.toString();	
	}
	public void walk(FileTreeWalker walker) {
		super._walk(walker);
	}
	public void setDirsModified() {
		super.updateDirModifiedTime();
	}

	/**
	 * as Filetee does not have a filename of length 1, it has to handle source and target differently 
	 */
	private Path temptarget;
	@Override
	public Path getTargetPath() {
		return temptarget == null ? super.getTargetPath() : temptarget;
	}
	public void walkCompleted(Config config) {
		locator = null;
		temptarget = config == null ? null : config.getTarget(); 
		update(temptarget);

		if(RootConfig.backupDriveFound()) 
			setTarget(temptarget);
	}

}
