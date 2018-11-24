package sam.backup.manager.file.db;

import static sam.backup.manager.file.db.AttributeMeta.ATTR_TABLE_NAME;
import static sam.backup.manager.file.db.FileMeta.BACKUP_ATTR;
import static sam.backup.manager.file.db.FileMeta.DIRS_TABLE_NAME;
import static sam.backup.manager.file.db.FileMeta.FILENAME;
import static sam.backup.manager.file.db.FileMeta.FILES_TABLE_NAME;
import static sam.backup.manager.file.db.FileMeta.ID;
import static sam.backup.manager.file.db.FileMeta.PARENT_ID;
import static sam.backup.manager.file.db.FileMeta.SRC_ATTR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.application.Platform;
import sam.backup.manager.extra.TreeType;
import sam.sql.SqlConsumer;
import sam.sql.sqlite.SQLiteDB;
import sam.string.BasicFormat;

public final class FileTree extends Dir {
	
	private final DirList dirList;
	private final FileList fileList;
	
	private TreeType treetype;
	private final Path srcPath;
	private Path backupPath;
	private Attrs srcAttrs, backupAttrs;

	public static final Attr DEFAULT_ATTR = new Attr(0, 0, 0);

	FileTree(TreeType type, Path sourceDirPath, Path backupDirPath) throws IOException {
		super(null, 0, null, sourceDirPath.toString(), defaultAttrs(), defaultAttrs());
		this.treetype = Objects.requireNonNull(type);
		this.srcPath = sourceDirPath;
		this.backupPath = backupDirPath;
	}
	private static Attrs defaultAttrs() {
		return new Attrs(DEFAULT_ATTR);
	}
	@Override public Attrs getSourceAttrs() { return srcAttrs; }
	@Override public Attrs getBackupAttrs() { return backupAttrs; }
	
	private String srcPathString, backupPathString;
	@Override 
	public String getSourcePath() { 
		return srcPathString != null ? srcPathString : ( srcPathString = srcPath.toString()); 
	}
	@Override
	public String getBackupPath() {
		return backupPathString != null ? backupPathString : ( backupPathString = backupPath == null ? "" : backupPath.toString());
	}
	public TreeType getTreetype(){ return this.treetype; }
	
	
	public List2<FileImpl> getFiles() {
		return files;
	}
	public List2<Dir> getDirs() {
		return dirs;
	}
	public void forcedMarkUpdated() {
		files.forEach(f -> f.getSourceAttrs().setUpdated());
	}
	public void backupRemove(FilteredFileTree delete, BiConsumer<FileImpl, Boolean> resultconsumer) {
		FileImpl fte = iter.next();
		File file = fte.getBackupPath().toFile();
		boolean b = !file.exists() || file.delete();
		if(b) fte.remove();

		String s = file.toString();
		if(root != null && s.length() > root.length() && s.startsWith(root))
			s = s.substring(root.length());

		if(b || !fte.isDirectory()) {
			if(b)
				success++;
			total++;
			sb.append(b).append("  ").append(s).append('\n');
		}

		if(System.currentTimeMillis() >= time) {
			time = System.currentTimeMillis() + 1000;
			String ss = sb.toString();
			sb.setLength(0);
			String st = success+"/"+total;
			Platform.runLater(() -> {
				d.view.appendText(ss);
				d.text.setText(st);
			});
		}
	}
	public FileImpl newFile(Dir parent, String filename) {
		return fileList.add(parent, filename);
	}
	public Dir newDir(Dir parent, String filename) {
		return dirList.add(this, parent, filename);
	}
}

