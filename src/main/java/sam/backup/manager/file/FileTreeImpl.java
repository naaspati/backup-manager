package sam.backup.manager.file;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeEditor;
import sam.backup.manager.file.api.FileTreeWalker;
import sam.backup.manager.file.api.FilteredDir;
import sam.backup.manager.walk.WalkMode;
import sam.functions.IOExceptionConsumer;
import sam.io.BufferSupplier;
import sam.io.IOUtils;
import sam.io.serilizers.StringIOUtils;
import sam.myutils.Checker;
import sam.nopkg.Junk;
import sam.nopkg.Resources;

final class FileTreeImpl implements FileTree, Dir {
	private static final Logger logger = LogManager.getLogger(FileTreeImpl.class);

	private final FileImpl[] data;
	private FileImpl[] new_data;
	private int nextId;

	private final PathWrap srcPath;
	private PathWrap backupPath;
	private final BitSet dirWalked;
	public final int tree_id;
	private final Path saveDir;
	private final BitSet attrsMod = new BitSet();
	private final BitSet isDir;

	private FileTreeImpl(int tree_id, Path saveDir, String[] filenames, int[] parents, BitSet isDir, Attr[] srcAttrs,
			Attr[] backupAttrs, Path sourceDirPath, Path backupDirPath) throws IOException {
		Checker.requireNonNull("filenames, parents, isDir, srcAttrs, backupAttrs, sourceDirPath, backupDirPath",
				filenames, parents, isDir, srcAttrs, backupAttrs, sourceDirPath, backupDirPath);

		this.isDir = isDir;
		this.tree_id = tree_id;
		this.saveDir = saveDir;

		this.srcPath = new PathWrap(sourceDirPath);
		this.backupPath = new PathWrap(backupDirPath);

		data = new FileImpl[filenames.length];
		IntFunction<DirImpl> parent = id -> (DirImpl) data[parents[id]];

		for (int id = 0; id < filenames.length; id++) {
			if (isDir.get(id))
				data[id] = new DirImpl(id, parent.apply(id), filenames[id], srcAttrs[id], backupAttrs[id]);
			else
				data[id] = new FileImpl(id, parent.apply(id), filenames[id], srcAttrs[id], backupAttrs[id]);
		}

		nextId = data.length;
		dirWalked = new BitSet(nextId + 100);
	}

	private FileTreeImpl(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath) {
		this.srcPath = PathWrap.of(Objects.requireNonNull(sourceDirPath));
		this.backupPath = PathWrap.of(Objects.requireNonNull(backupDirPath));

		this.saveDir = saveDir;
		this.tree_id = tree_id;
		this.data = new FileImpl[0];
		this.nextId = 0;
		this.isDir = new BitSet(200);
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
				return dirWalked.get(id(dir));
			}

			@Override
			public FileImpl addFile(Dir parent, String filename) {
				return add(parent,
						new FileImpl(fileSerial.next(), (DirImpl) parent, filename, EMPTY_ATTRS, EMPTY_ATTRS));
			}

			@SuppressWarnings("unchecked")
			private <E extends FileImpl> E add(Dir parent, E file) {
				return (E) dir(parent).add(file);
			}

			@Override
			public DirImpl addDir(Dir parent, String dirname) {
				return add(parent, new DirImpl(dirSerial.next(), (DirImpl) parent, dirname, EMPTY_ATTRS, EMPTY_ATTRS,
						EMPTY_ARRAY));
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
	public PathWrap getSourcePath() {
		return srcPath;
	}

	@Override
	public PathWrap getBackupPath() {
		return backupPath;
	}

	public void forcedMarkUpdated() {
		// TODO serializer.applyToAll(f -> f.getSourceAttrs().setUpdated());
		Junk.notYetImplemented();
	}

	@Override
	public Dir getParent() {
		return null;
	}

	@Override
	public Attrs getBackupAttrs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attrs getSourceAttrs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public Status getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSourceSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<FileEntity> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int childrenCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void walk(FileTreeWalker walker) {
		// TODO Auto-generated method stub

	}

	@Override
	public FilteredDir filtered(Predicate<FileEntity> filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int countFilesInTree() {
		// TODO Auto-generated method stub
		return 0;
	}
	public void save() throws IOException {
		if (attrsMod.isEmpty())
			return;

		TreePaths t = new TreePaths(tree_id, saveDir);
		
		try (Resources r = Resources.get()) {
			if (nextId == data.length) {
				logger.debug("no new filenames to save: ", this);
			} else {
				new MetaHandler(t.meta, tree_id).write(nextId, data, r);
				new FileNamesHandler(t.filenamesPath).write(r, nextId, data, new_data);
				new RemainingHandler(t.remainingPath).write(nextId, data, new_data, isDir, r);
			}
			new AttrsHandler(t.attrsPath).write(attrsMod, data, r.buffer());
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
}