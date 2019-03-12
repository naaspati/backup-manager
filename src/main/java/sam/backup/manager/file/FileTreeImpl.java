package sam.backup.manager.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Objects;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeEditor;
import sam.backup.manager.file.api.Status;
import sam.backup.manager.file.api.Type;
import sam.backup.manager.walk.WalkMode;
import sam.myutils.Checker;
import sam.nopkg.Junk;
import sam.nopkg.Resources;

final class FileTreeImpl extends DirImpl implements FileTree, Dir {
	private static final Logger logger = LogManager.getLogger(FileTreeImpl.class);

	private final ArrayWrap<FileImpl> files;
	private final Aw srcAttrs;
	private final Aw backupAttrs;

	private final PathWrap srcPath;
	private PathWrap backupPath;
	private final BitSet dirWalked;
	public final int tree_id;
	private final Path saveDir;
	private final BitSet attrsMod = new BitSet();
	private final BitSet status = new BitSet();
	
	private final DirHelper helper = new DirHelper() {
		@Override
		public Status statusOf(int id) {
			return new Stat(id);
		}
		@Override
		public Attr attr(int id, Type type) {
			return attr0(id, type);
		}
		@Override
		public FileEntity file(int id) {
			return files.get(id);
		}
	};
	
	private class Aw extends ArrayWrap<Attr> {
		public Aw(Attr[] data) {
			super(data);
		}
		@Override
		public void set(int id, Attr e) {
			super.set(id, e);
			attrsMod.set(id);
		}
		
	}

	private FileTreeImpl(int tree_id, Path saveDir, String[] filenames, int[] parents, BitSet isDir, Attr[] srcAttrs,
			Attr[] backupAttrs, Path sourceDirPath, Path backupDirPath) throws IOException {
		super(0, sourceDirPath.toString(), null, null);
		
		Checker.requireNonNull("filenames, parents, isDir, srcAttrs, backupAttrs, sourceDirPath, backupDirPath",
				filenames, parents, isDir, srcAttrs, backupAttrs, sourceDirPath, backupDirPath);

		this.tree_id = tree_id;
		this.saveDir = saveDir;

		this.srcPath = new PathWrap(sourceDirPath);
		this.backupPath = new PathWrap(backupDirPath);

		FileImpl[] data = new FileImpl[filenames.length];
		IntFunction<DirImpl> parent = id -> (DirImpl) data[parents[id]];

		for (int id = 0; id < filenames.length; id++) {
			if (isDir.get(id))
				data[id] = new DirImpl(id, filenames[id], parent.apply(id), helper);
			else
				data[id] = new FileImpl2(id, filenames[id], parent.apply(id));
		}
		
		this.files = new ArrayWrap<>(data);
		this.srcAttrs = new Aw(srcAttrs);
		this.backupAttrs = new Aw(backupAttrs);
		dirWalked = new BitSet(files.size() + 100);
	}

	private FileTreeImpl(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath) {
		super(0, sourceDirPath.toString(), null, null);
		
		this.srcPath = PathWrap.of(Objects.requireNonNull(sourceDirPath));
		this.backupPath = PathWrap.of(Objects.requireNonNull(backupDirPath));

		this.saveDir = saveDir;
		this.tree_id = tree_id;
		this.files = new ArrayWrap<>(new FileImpl[]{this});
		this.srcAttrs = new Aw(new Attr[]{null});
		this.backupAttrs = new Aw(new Attr[]{null});
		this.dirWalked = new BitSet(200);
	}

	@Override
	public FileTreeDeleter getDeleter() {
		return new FileTreeDeleter() {

			@Override
			public void delete(FileEntity f, PathWrap file) throws IOException {
				if (file != null)
					Files.deleteIfExists(file.path());
				// if file is null, only remove from
				// TODO Auto-generated method stub
				Junk.notYetImplemented();
			}

			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	public FileTreeEditor getEditor(Path start) {
		return new FileTreeEditor() {
			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub

			}

			@Override
			public Dir addDir(Path dir, Attr attr, WalkMode walkMode) {
				// FIXME
				return Junk.notYetImplemented();
			}

			@Override
			public void setAttr(Attr attr, WalkMode walkMode, Path dir) {
				// FIXME
				Junk.notYetImplemented();
			}

			@Override
			public FileEntity addFile(Path file, Attr af, WalkMode walkMode) {
				// FIXME
				return Junk.notYetImplemented();
			}

			@Override
			public void setWalked(Dir dir, boolean walked) {
				dirWalked.set(cast(dir).id, walked);
			}

			@Override
			public boolean isWalked(Dir dir) {
				return dirWalked.get(cast(dir).id);
			}

			@Override
			public FileImpl addFile(Dir parent, String filename) {
				return Junk.notYetImplemented();// FIXME add(parent, new FileImpl(fileSerial.next(), (DirImpl) parent, filename, EMPTY_ATTRS, EMPTY_ATTRS));
			}

			@SuppressWarnings("unchecked")
			private <E extends FileImpl> E add(Dir parent, E file) {
				return Junk.notYetImplemented();// FIXME (E) dir(parent).add(file);
			}

			@Override
			public DirImpl addDir(Dir parent, String dirname) {
				return Junk.notYetImplemented();// FIXME add(parent, new DirImpl(dirSerial.next(), (DirImpl) parent, dirname, EMPTY_ATTRS, EMPTY_ATTRS, EMPTY_ARRAY));
			}
		};
	}

	protected FileImpl cast(Object o) {
		return (FileImpl) o;
	}

	@Override
	public boolean isWalked(Dir dir) {
		return dirWalked.get(cast(dir).id);
	}

	@Override
	public PathWrap getPath(Type type) {
		switch (type) {
			case BACKUP: return backupPath;
			case SOURCE: return srcPath;
			default:
				throw new NullPointerException();
		}
	}

	public void forcedMarkUpdated() {
		// TODO serializer.applyToAll(f -> f.getSourceAttrs().setUpdated());
		Junk.notYetImplemented();
	}
	@Override
	public DirImpl getParent() {
		return null;
	}

	@Override
	protected Attr attr(Type type) {
		return attr0(0, type);
	}
	@Override
	protected DirHelper helper() {
		return helper;
	}
	
	public void save() throws IOException {
		if (attrsMod.isEmpty())
			return;

		TreePaths t = new TreePaths(tree_id, saveDir);
		
		try (Resources r = Resources.get()) {
			if (!files.isModified()) {
				logger.debug("no new filenames to save: ", this);
			} else {
				new MetaHandler(t.meta, tree_id).write(files, r);
				new FileNamesHandler(t.filenamesPath).write(r, files);
				new RemainingHandler(t.remainingPath).write(files, r);
			}
			new AttrsHandler(t.attrsPath).write(attrsMod, files, srcAttrs, backupAttrs, r.buffer());
		}
	}

	public static FileTreeImpl read(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath)
			throws IOException {

		TreePaths t = new TreePaths(tree_id, saveDir);
		t.existsValidate();

		final BitSet isDir;
		final int[] parents;
		final Attr[] src;
		final Attr[] backup;
		final String[] filenames;

		try (Resources r = Resources.get()) {
			final int count = new MetaHandler(t.meta, tree_id).validate(r, sourceDirPath, backupDirPath);
			filenames =  new FileNamesHandler(t.filenamesPath).read(r, count);
			AttrsHandler attrs = new AttrsHandler(t.attrsPath);
			attrs.read(r, filenames);

			src = attrs.src;
			backup = attrs.backup;
			
			attrs.src = null;
			attrs.backup = null;
			
			RemainingHandler rh = new RemainingHandler(t.remainingPath);
			rh.read(r.buffer());
			
			isDir = rh.isDir;
			parents = rh.parents;
			
			rh.isDir = null;
			rh.parents = null;
		}

		return new FileTreeImpl(tree_id, saveDir, filenames, parents, isDir, src, backup, sourceDirPath, backupDirPath);
	}
	
	private class Stat implements Status2 {
		private final int id;
		private String reason;

		public Stat(int id) {
			this.id = id;
		}
		@Override
		public String getBackupReason() {
			return reason;
		}
		int index(int type) {
			return id * SIZE + type;
		}
		@Override
		public void set(int type, boolean value) {
			status.set(index(type), value);
		}
		@Override
		public boolean get(int type) {
			return status.get(index(type));
		}
		@Override
		public void setBackupReason(String reason) {
			this.reason = reason;
		}
	}
	
	public class FileImpl2 extends FileImpl {
		final DirImpl parent;
		Stat status;
		
		public FileImpl2(int id, String filename, DirImpl parent) {
			super(id, filename);
			this.parent = parent;
		}
		@Override
		public DirImpl getParent() {
			return parent;
		}

		@Override
		public Status getStatus() {
			if(status == null)
				status = new Stat(id);
			
			return status;
		}

		@Override
		protected Attr attr(Type type) {
			return attr0(id, type);
		}
	}

	public Attr attr0(int id, Type type) {
		switch (type) {
			case BACKUP: return backupAttrs.get(id);
			case SOURCE: return srcAttrs.get(id);
			default:
				throw new NullPointerException();
		}
	}
}