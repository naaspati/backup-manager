package sam.backup.manager.file;

import static sam.backup.manager.file.WithId.id;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

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
import sam.collection.IntSet;
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
	private final ChildrenImpl children = new ChildrenImpl();

	public class ChildrenImpl implements Children {
		private final IntSet set = new IntSet();
		private int mod;
		
		@Override
		public int mod() {
			return mod;
		}
		public void ensureNotMod(int expectedMod) {
			if(expectedMod != mod)
				throw new ConcurrentModificationException();
		}

		@Override
		public Iterator<FileImpl> iterator() {
			if(set.isEmpty())
				return Collections.emptyIterator();
			int modm = this.mod;

			return new Iterator<FileImpl>() {
				int n = 0;

				@Override
				public FileImpl next() {
					ensureNotMod(modm);
					
					if(n >= size())
						throw new NoSuchElementException();

					return files.get(set.get(n++));
				}
				@Override
				public boolean hasNext() {
					return n < set.size();
				}
			};
		}

		@Override
		public int size() {
			return set.size();
		}
	}

	private final FileHelper fileHelper = new FileHelper() {
		@Override
		public Status statusOf(FileImpl file) {
			return new Stat(file.getId());
		}
		@Override
		public Attr attr(FileImpl file, Type type) {
			return attr0(file.getId(), type);
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

	private FileTreeImpl(int tree_id, FileImpl[] data, Path saveDir, Attr[] srcAttrs, Attr[] backupAttrs, Path sourceDirPath, Path backupDirPath) throws IOException {
		super(0, sourceDirPath.toString(), null, null, null);

		Checker.requireNonNull("filenames, parents, isDir, srcAttrs, backupAttrs, sourceDirPath, backupDirPath", srcAttrs, backupAttrs, sourceDirPath, backupDirPath);

		this.tree_id = tree_id;
		this.saveDir = saveDir;

		this.srcPath = new PathWrap(sourceDirPath);
		this.backupPath = new PathWrap(backupDirPath);

		this.files = new ArrayWrap<>(data);
		this.srcAttrs = new Aw(srcAttrs);
		this.backupAttrs = new Aw(backupAttrs);
		dirWalked = new BitSet(files.size() + 100);
	}

	private FileTreeImpl(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath) {
		super(0, sourceDirPath.toString(), null, null, null);

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
				dirWalked.set(id(dir), walked);
			}

			@Override
			public boolean isWalked(Dir dir) {
				return dirWalked.get(id(dir));
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

	@Override
	public boolean isWalked(Dir dir) {
		return dirWalked.get(id(dir));
	}

	public void forcedMarkUpdated() {
		// TODO serializer.applyToAll(f -> f.getSourceAttrs().setUpdated());
		Junk.notYetImplemented();
	}
	@Override
	public DirImpl getParent() {
		return null;
	}
	public Attr attr0(int id, Type type) {
		switch (type) {
			case BACKUP: return backupAttrs.get(id);
			case SOURCE: return srcAttrs.get(id);
			default:
				throw new NullPointerException();
		}
	}
	
	@Override
	public PathWrap getSourcePath() {
		return srcPath;
	}
	@Override
	public PathWrap getBackupPath() {
		return backupPath;
	}
	@Override
	public Children children() {
		return children;
	}
	@Override
	protected FileHelper fileHelper() {
		return fileHelper;
	}

	public void save() throws IOException {
		if (attrsMod.isEmpty())
			return;

		TreePaths t = new TreePaths(tree_id, saveDir);

		try (Resources r = Resources.get()) {
			if (!files.isModified()) {
				logger.debug("no new filenames to save: ", this);
			} else {
				new MetaHandler(t.meta, tree_id).write(files, r, srcPath.path(), backupPath.path());
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

		try (Resources r = Resources.get()) {
			final int count = new MetaHandler(t.meta, tree_id).validate(r, sourceDirPath, backupDirPath);
			String[] filenames =  new String[count];
			new FileNamesHandler(t.filenamesPath).read(r, new Consumer<String>() {
				int n = 0;
				@Override
				public void accept(String t) {
					filenames[n++] = Checker.isEmpty(t) ? null : t;
				}
			});
			
			AttrsHandler attrs = new AttrsHandler(t.attrsPath);
			attrs.read(r, filenames);

			final Attr[] src = attrs.src;
			final Attr[] backup = attrs.backup;

			attrs.src = null;
			attrs.backup = null;

			RemainingHandler rh = new RemainingHandler(t.remainingPath);
			rh.read(r.buffer());

			BitSet isDir = rh.isDir;
			int[] parents = rh.parents;
			int[] sizes = rh.sizes;

			rh.isDir = null;
			rh.parents = null;
			rh.sizes = null;

			FileImpl[] files = new FileImpl[filenames.length];
			FileTreeImpl tree = new FileTreeImpl(tree_id, files, saveDir, src, backup, sourceDirPath, backupDirPath);
			files[0] = tree;
			
			for (int id = 1; id < files.length; id++) {
				String name = filenames[id];
				DirImpl parent = (DirImpl) files[parents[id]];

				if (isDir.get(id))
					files[id] = new DirImpl(id, name, parent, tree.fileHelper, tree.newChildren());
				else
					files[id] = new FileImpl(id, name, parent, tree.fileHelper);
			}
			
			int n = 0; 
			while(n < sizes.length) 
				children(files[sizes[n++]]).set.ensureCapacity(sizes[n++] + 2);
			
			return tree;
		}
	}

	private static ChildrenImpl children(FileImpl f) {
		return (ChildrenImpl)(((DirImpl)f).children());
	}

	private Children newChildren() {
		return new ChildrenImpl();
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
}