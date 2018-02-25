package sam.backup.manager.file;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static sam.backup.manager.walk.WalkType.BACKUP;
import static sam.backup.manager.walk.WalkType.NEW_SOURCE;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import sam.backup.manager.config.Config;
import sam.backup.manager.walk.WalkType;

public class FileTree implements Serializable {
	private static final long serialVersionUID = -3216725012618093594L;

	private final String pathString;
	private final List<FileTree> children;
	private long modifiedTime = -1; 
	private final boolean isDirectory;

	private transient boolean copied;
	private transient boolean backupNeeded, deleteBackup;
	private transient String backupReason;
	private transient Path path;
	private transient Path fullPath;
	private transient Path target;
	private transient AboutFile sourceAF, backupAF;

	public static FileTree read(Path path) throws IOException {
		try(InputStream is = Files.newInputStream(path);
				DataInputStream dis = new DataInputStream(is);) {
			String versionString = "FileTree: version:1.0";
			String s = dis.readUTF();

			if(!Objects.equals(s, versionString))
				throw new IOException("not a filetree file");

			return new FileTree(dis); 
		}
	}
	public static void write(Path path, FileTree tree) throws IOException {
		try(OutputStream is = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				DataOutputStream dos = new DataOutputStream(is);) {
			String versionString = "FileTree: version:1.0";
			dos.writeUTF(versionString);
			tree.write(dos); 
		}
	}
	private FileTree(DataInputStream dis) throws IOException {
		pathString = dis.readUTF();
		modifiedTime = dis.readLong();
		isDirectory = dis.readBoolean();
		if(isDirectory) {
			int size = dis.readInt();
			children = new ArrayList<>(size);
			while(size-- > 0) 
				children.add(new FileTree(dis));
		} else 
			children = null;
	}
	private void write(DataOutputStream dos) throws IOException {
		dos.writeUTF(pathString);
		dos.writeLong(modifiedTime);
		dos.writeBoolean(isDirectory);
		if(isDirectory) {
			if(children == null) {
				dos.writeInt(0);
				return;
			}

			dos.writeInt(children.size());
			for (FileTree f : children) f.write(dos);
		}
	}
	public FileTree(Path fileName) {
		this(fileName, true);
	}
	private FileTree(Path path, boolean isDirectory) {
		this.pathString = path.toString();
		this.path = path;
		this.isDirectory = isDirectory;
		children = isDirectory ? new ArrayList<>() : null;
	}
	public List<FileTree> getChildren() {
		return children;
	}
	public Path getFileName() {
		return path = path != null ? path : Paths.get(pathString);
	}
	public Path getSourcePath() {
		return fullPath;
	}
	public boolean isBackupNeeded() {
		if(isDirectory)
			return children.stream().anyMatch(FileTree::isBackupNeeded);

		return backupNeeded;
	}

	public void setBackupNeeded(boolean backupNeeded, String reason) {
		this.backupNeeded = backupNeeded;
		this.backupReason = reason;
	}
	public boolean isDeleteBackup() {
		if(isDirectory)
			return children.stream().anyMatch(FileTree::isDeleteBackup);
		return deleteBackup;
	}
	public void setDeleteBackup(boolean deleteBackup) {
		this.deleteBackup = deleteBackup;
	}
	public String getBackupReason() {
		return backupReason;
	}
	public boolean isCopied() {
		return copied;
	}
	public void setCopied() {
		copied = true;
		if(sourceAF != null)
			modifiedTime = sourceAF.getModifiedTime();
	}
	public boolean isDirectory() {
		return isDirectory;
	}
	public AboutFile getBackupAboutFile() {
		return backupAF;
	}
	public AboutFile getSourceAboutFile() {
		return sourceAF;
	}
	public FileTree addFile(Path partialPath, Path fullpath, AboutFile aboutFile, WalkType walkType) {
		return add(partialPath, fullpath, aboutFile, walkType, false);
	}
	public FileTree addDirectory(Path partialPath, Path fullpath, AboutFile aboutFile, WalkType walkType) {
		return add(partialPath, fullpath, aboutFile, walkType, true);
	}
	private FileTree add(Path partialPath, Path fullpath, AboutFile aboutFile, WalkType walkType, boolean isDirectory) {
		if(partialPath.getNameCount() == 1) {
			if(children != null && walkType != NEW_SOURCE) {
				for (FileTree ft : children) {
					if(ft.getFileName().equals(partialPath)) {
						ft.setAboutFile(aboutFile, walkType, fullpath);
						return ft;
					}
				}
			}
			FileTree ft = addChild(new FileTree(partialPath, isDirectory));
			ft.setAboutFile(aboutFile, walkType, fullpath);
			return ft;
		}
		else {
			Path p2 = partialPath.getName(0);
			for (FileTree ft : children) {
				if(p2.equals(ft.getFileName())) {
					ft.add(partialPath.subpath(1, partialPath.getNameCount()),fullpath, aboutFile,walkType, isDirectory);
					return ft;
				} 
			}
		}
		throw new IllegalStateException("no parent found for name: "+partialPath+"  fullpath: "+fullpath);
	}
	private FileTree addChild(FileTree child){
		children.add(child);
		return child;
	}
	public void setAboutFile(AboutFile aboutFile, WalkType walkType, Path fullpath) {
		if(walkType == BACKUP)
			backupAF = aboutFile;
		else {
			sourceAF = aboutFile ;
			this.fullPath = fullpath;
		}
	}
	private void append(final char[] separator, final StringBuilder sb, Predicate<FileTree> filter) {
		if(children == null || children.isEmpty()) return;

		for (FileTree f : children) {
			if(!filter.test(f))
				continue;
			sb.append(separator).append(f.pathString).append('\n');

			if(f.isDirectory) {
				int length = separator.length;
				char[] sp2 = Arrays.copyOf(separator, length + 6);
				Arrays.fill(sp2, length, sp2.length, ' ');
				if(length != 2) {
					sp2[length - 1] = ' ';
					sp2[length - 2] = ' ';
				}
				sp2[sp2.length - 3] = '|';
				sp2[sp2.length - 2] = '_';
				sp2[sp2.length - 1] = '_';
				f.append(sp2, sb, filter);
			}
		}
	}
	public String toTreeString() {
		return toTreeString(p -> true);
	}
	public String toTreeString(Predicate<FileTree> filter) {
		StringBuilder sb = new StringBuilder().append(pathString).append('\n');
		append(new char[] {' ', '|'}, sb, filter);
		return sb.toString();	
	}
	@Override
	public String toString() {
		return pathString;
	}
	public long getSourceSize() {
		return sourceAF.getSize();
	}
	public Path getTargetPath() {
		return target;
	}
	public void calculateTargetPath(Config config) {
		_calculateTargetPath(config.getTarget());
	}
	private void calculateTargetPath(Path p) {
		this.target = p.resolve(getFileName());
		_calculateTargetPath(this.target); 
	}
	private void _calculateTargetPath(Path p) {
		if(children == null || children.isEmpty())
			return;

		for (FileTree f : children) 
			f.calculateTargetPath(p); 
	}
	public long getModifiedTime() {
		return modifiedTime;
	}
	public void walk(FileTreeWalker walker) {
		_walk(walker);
	}
	private FileVisitResult _walk(FileTreeWalker walker) {
		for (FileTree f : children) {
			if(f.isDirectory && (f.children == null || f.children.isEmpty()))
				continue;

			FileVisitResult result = f.isDirectory ? walker.dir(f, f.sourceAF, f.backupAF) : walker.file(f, f.sourceAF, f.backupAF);

			if(result == TERMINATE)
				return TERMINATE;

			if(result == SKIP_SIBLINGS || result == SKIP_SUBTREE)
				return CONTINUE;

			if(f.isDirectory && f._walk(walker) == TERMINATE)
				return TERMINATE;
		}
		return CONTINUE;
	}
	public boolean isNew() {
		return modifiedTime == -1;
	}
	public void setDirModifiedTime() {
		setCopied();
		if(children == null)
			return;
		for (FileTree f : children)
			f.setDirModifiedTime();
	}
}

