package sam.backup.manager.file;

import static sam.backup.manager.file.api.WithId.id;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.AbstractDir;
import sam.backup.manager.file.api.AbstractFileEntity;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeEditor;
import sam.backup.manager.file.api.FilteredDir;
import sam.backup.manager.file.api.FilteredDirImpl;
import sam.backup.manager.file.api.Status;
import sam.backup.manager.file.api.Type;
import sam.backup.manager.file.api.WithId;
import sam.backup.manager.walk.WalkMode;
import sam.collection.IntSet;
import sam.myutils.Checker;
import sam.myutils.ThrowException;
import sam.nopkg.Junk;

final class FileTreeImpl extends AbstractDir implements FileTree, Dir {
    private static final Logger logger = LogManager.getLogger(FileTreeImpl.class);

    private ArrayWrap<AbstractFileEntity> files;
    private Aw srcAttrs;
    private Aw backupAttrs;
    private DirImpl me;

    private final PathWrap srcPath;
    private PathWrap backupPath;
    private final BitSet dirWalked;
    public final int tree_id;
    private final Path saveDir;
    private final BitSet attrsMod = new BitSet();
    private final BitSet status = new BitSet();

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

    private FileTreeImpl(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath, String init_by_read) throws IOException {
        super(sourceDirPath.toString());

        Checker.requireNonNull("filenames, parents, isDir, srcAttrs, backupAttrs, sourceDirPath, backupDirPath", srcAttrs, backupAttrs, sourceDirPath, backupDirPath);

        this.tree_id = tree_id;
        this.saveDir = saveDir;

        this.srcPath = new PathWrap(sourceDirPath);
        this.backupPath = new PathWrap(backupDirPath);
        this.dirWalked = new BitSet(files.size() + 100);
    }

    private FileTreeImpl(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath) {
        super(sourceDirPath.toString());

        this.srcPath = PathWrap.of(Objects.requireNonNull(sourceDirPath));
        this.backupPath = PathWrap.of(Objects.requireNonNull(backupDirPath));

        this.me = new DirImpl(0, filename, -1, -1);
        this.saveDir = saveDir;
        this.tree_id = tree_id;
        this.files = new ArrayWrap<>(new AbstractFileEntity[]{this});
        this.srcAttrs = new Aw(new Attr[]{null});
        this.backupAttrs = new Aw(new Attr[]{null});
        this.dirWalked = new BitSet(200);
    }

    @Override
    protected Attr attr(Type type) {
        return me.attr(type);
    }
    @Override
    public int childrenCount() {
        return me.childrenCount();
    }
    @Override
    public Status getStatus() {
        return me.getStatus();
    }
    @Override
    public Iterator<FileEntity> iterator() {
        return me.iterator();
    }
    @Override
    public FilteredDir filtered(Predicate<FileEntity> filter) {
        return me.filtered(filter);
    }

    @Override
    public FileTreeDeleter getDeleter() {
        return new FileTreeDeleter() {

            @Override
            public void delete(FileEntity f, Type type) throws IOException {
                if(f == null)
                    return;

                if(type == Type.SOURCE)
                    ThrowException.illegalArgumentException("type == Type.SOURCE not supported");

                PathWrap p = f.getBackupPath();

                if (p != null)
                    Files.deleteIfExists(p.path());

                dir(f.getParent()).remove(f);
            }

            @Override
            public void close() throws IOException {
                // TODO Auto-generated method stub
            }
        };
    }

    static DirImpl dir(FileEntity f) {
        return (DirImpl)f;
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
    public void save() throws IOException {
        if (attrsMod.isEmpty())
            return;

        TreePaths t = new TreePaths(tree_id, saveDir);

        Serializer s = serializer();
        s.t = t;
        s.files = files;
        s.srcPath = srcPath;
        s.backupPath = backupPath;

        try {
            s.save();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private Serializer serializer() {
        return new Serializer() {

            @Override
            protected int id(FileEntity f) {
                if(f == null)
                    return -1;
                else 
                    return WithId.id(f);
            }

            @Override
            protected AbstractFileEntity newFile(int id, int parent_id, String filename) {
                return new FileImpl(parent_id, filename, parent_id);
            }

            @Override
            protected AbstractDir newDir(int id, int parent_id, int child_count, String filename) {
                return new DirImpl(id, filename, parent_id, child_count);
            }
            @Override
            protected void addChild(AbstractFileEntity parent, AbstractFileEntity child) {
                ((DirImpl)parent).set.add(id(child));
            }
        };
    }

    public static FileTreeImpl read(int tree_id, Path saveDir, Path sourceDirPath, Path backupDirPath) throws IOException {
        TreePaths t = new TreePaths(tree_id, saveDir);
        t.existsValidate();

        FileTreeImpl tree = new FileTreeImpl(tree_id, saveDir, sourceDirPath, backupDirPath, null);
        Serializer s = tree.serializer();

        s.t = t;
        s.srcPath = PathWrap.of(sourceDirPath);
        s.backupPath = PathWrap.of(backupDirPath);

        try {
            s.read();
            tree.me = (DirImpl) s.files.get(0);
            s.files.set(0, tree);

            tree.files = s.files;
            tree.srcAttrs = tree.aw(s.srcAttrs);
            tree.backupAttrs = tree.aw(s.backupAttrs);

            return tree;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private Aw aw(Attr[] data) {
        return new Aw(data);
    }

    private class FileImpl extends AbstractFileEntity implements WithId {
        private final int id, parent_id;
        private Stat stat;

        protected FileImpl(int id, String filename, int parent_id) {
            super(filename);
            this.id = id;
            this.parent_id = parent_id;
        }

        @Override
        public Dir getParent() {
            return (Dir) files.get(parent_id);
        }

        @Override
        public Status getStatus() {
            if(stat == null)
                stat = new Stat(id);
            return stat;
        }

        @Override
        protected Attr attr(Type type) {
            return attr0(id, type);
        }

        @Override
        public int getId() {
            return id;
        }
    }

    private class DirImpl extends AbstractDir implements WithId {
        private final IntSet set;
        private int mod;
        private final int id, parent_id;
        private Stat stat;

        protected DirImpl(int id, String filename, int parent_id, int child_count) {
            super(filename);
            this.id = id;
            this.parent_id = parent_id;
            this.set = child_count < 0 ? new IntSet() : new IntSet(child_count);
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public Dir getParent() {
            return (Dir) files.get(parent_id);
        }

        private void remove(FileEntity f) {
            if(f == null)
                return;

            int id = id(f);
            if(set.remove(id)) {
                mod++;
                files.set(id, null);
            }
        }

        public void ensureNotMod(int expectedMod) {
            if(expectedMod != mod)
                throw new ConcurrentModificationException();
        }

        @Override
        public Iterator<FileEntity> iterator() {
            if(set.isEmpty())
                return Collections.emptyIterator();
            int modm = this.mod;

            return new Iterator<FileEntity>() {
                int n = 0;

                @Override
                public FileEntity next() {
                    ensureNotMod(modm);

                    if(n >= set.size())
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
        public int childrenCount() {
            return set.size();
        }
        @Override
        public FilteredDir filtered(Predicate<FileEntity> filter) {
            return new FilteredDirImpl2(this, null, filter);
        }
        @Override
        public Status getStatus() {
            if(stat == null)
                stat = new Stat(id);
            return stat;
        }

        @Override
        protected Attr attr(Type type) {
            return attr0(id, type);
        }
    }

    private class FilteredDirImpl2 extends FilteredDirImpl {

        public FilteredDirImpl2(AbstractDir me, FilteredDirImpl parent, Predicate<FileEntity> filter) {
            super(me, parent, filter);
        }
        @Override
        protected FilteredDir newFilteredDirImpl(AbstractDir dir, FilteredDirImpl filtered, Predicate<FileEntity> filter) {
            return new FilteredDirImpl2(dir, filtered, filter);
        }

        @Override
        protected int mod(AbstractDir dir) {
            return ((DirImpl)dir).mod;
        }
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