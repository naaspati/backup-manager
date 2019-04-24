package sam.backup.manager.file;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.AbstractDirImpl;
import sam.backup.manager.file.api.AbstractFileImpl;
import sam.backup.manager.file.api.Attr;
import sam.backup.manager.file.api.Attrs;
import sam.backup.manager.file.api.Dir;
import sam.backup.manager.file.api.FileEntity;
import sam.backup.manager.file.api.FileTree;
import sam.functions.IOExceptionConsumer;
import sam.io.IOUtils;
import sam.io.serilizers.DataReader;
import sam.io.serilizers.DataWriter;
import sam.myutils.Checker;
import sam.nopkg.Resources;

abstract class Serializer {
    private static final long NULL_ATTR = -1574818154;
    private static final long EMPTY_ATTR = -432306467;

    ArrayWrap<AbstractFileImpl> files;
    public TreePaths t;
    public PathWrap srcPath, backupPath;
    public Attr[] srcAttrs;
    public Attr[] backupAttrs;

    public void save() throws IOException, NoSuchAlgorithmException {
        Checker.requireNonNull("files, t, srcPath, backupPath", files, t, srcPath, backupPath);

        if(!files.isModified())
            return;

        try(Resources r = Resources.get()) {
            Path nm = null;
            if(files.newSize() != 0)
                nm = appendNewNames(files, r);

            Path mt = writeMeta(files, r);

            if(nm != null)
                Files.move(nm, t.namesPath, REPLACE_EXISTING);

            Files.move(mt, t.metaPath, REPLACE_EXISTING);
        }
    }

    private Path write(Path src, StandardOpenOption option, IOExceptionConsumer<FileChannel> consumer) throws IOException {
        Path temp = src.resolveSibling(System.currentTimeMillis()+"-"+src.getFileName()+".tmp");
        Files.copy(src, temp);

        boolean success = false;

        try(FileChannel fc = FileChannel.open(temp, CREATE, WRITE, option);) {
            consumer.accept(fc);

            success = true;
            return temp;
        } finally {
            if(!success)
                Files.deleteIfExists(temp);
        }
    }

    private Path writeMeta(ArrayWrap<AbstractFileImpl> files, Resources r) throws IOException, NoSuchAlgorithmException {
        MessageDigest digestor = digestor();

        return write(t.metaPath, TRUNCATE_EXISTING, fc -> {
            ByteBuffer buf = r.buffer();
            buf.clear();

            buf.putInt(t.tree_id);
            digest(fc, buf, srcPath, digestor, r.encoder());
            digest(fc, buf, backupPath, digestor, r.encoder());

            writeIf(fc, buf, 4);
            buf.putInt(files.size());

            final int BYTES = Integer.BYTES * 2 + Attr.BYTES * 2;

            files.forEach(d -> {
                writeIf(fc, buf, BYTES);

                buf.putInt(id(d));
                buf.putInt(d.isDirectory() ? ((Dir)d).childrenCount() : -1);
                writeAttrs(d.getSourceAttrs(), buf);
                writeAttrs(d.getBackupAttrs(), buf);
            });

            IOUtils.write(buf, fc, true);
        });
    }

    private void writeIf(FileChannel fc, ByteBuffer buf, int num) throws IOException {
        if(buf.remaining() < num)
            IOUtils.write(buf, fc, true);
    }

    private void digest(FileChannel fc, ByteBuffer buf,  PathWrap p, MessageDigest d, CharsetEncoder e) throws IOException {
        byte[] digest = digest(p, d, e);

        writeIf(fc, buf, digest.length + 4);
        buf.putInt(digest.length);
        buf.put(digest);
    }

    private byte[] digest(PathWrap p, MessageDigest d, CharsetEncoder e) throws CharacterCodingException {
        d.reset();
        ByteBuffer b = e.encode(CharBuffer.wrap(p.string()));
        d.update(b.array(), b.position(), b.remaining());

        return d.digest();
    }

    private MessageDigest digestor() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5");
    }

    private void writeAttrs(Attrs s, ByteBuffer buf) {
        Attr a = s == null ? null : s.old();
        if(a == null) 
            buf.putLong(NULL_ATTR);
        else if(a == FileTree.EMPTY_ATTR)
            buf.putLong(EMPTY_ATTR);
        else {
            buf.putLong(a.lastModified);
            buf.putLong(a.size);    
        }
    }

    private Attr readAttrs(FileChannel fc, ByteBuffer buf) throws IOException {
        readIf(fc, buf, 8);
        long lm = buf.getLong();

        if(lm == NULL_ATTR)
            return null;
        else if(lm == EMPTY_ATTR)
            return FileTree.EMPTY_ATTR;
        else {
            readIf(fc, buf, 8);
            long s = buf.getLong();

            return new Attr(lm, s);
        }
    }

    private Path appendNewNames(ArrayWrap<AbstractFileImpl> files, Resources r) throws IOException {
        return write(t.namesPath, files.oldSize() == 0 ? TRUNCATE_EXISTING : APPEND, fc -> {
            try(DataWriter d = new DataWriter(fc, r.buffer())) {
                d.setEncoder(r.encoder());

                if(files.oldSize() == 0) {
                    d.writeInt(t.tree_id);
                    d.writeUTF(srcPath.string());
                    d.writeUTF(backupPath.string());
                }

                files.forEachNew(f -> {
                    d.writeBoolean(f.isDirectory());
                    d.writeInt(id(f));
                    d.writeInt(id(f.getParent()));
                    d.writeUTF(f.filename);
                });
            }            
        });
    }

    public void read() throws IOException, NoSuchAlgorithmException {
        Checker.requireNonNull("t, srcPath, backupPath", t, srcPath, backupPath);

        try(FileChannel meta = FileChannel.open(t.metaPath, READ); 
                Resources r = Resources.get();
                ) {
            ByteBuffer buf = r.buffer();

            if(IOUtils.read(buf, true, meta) < 4)
                throw new ValidationFailedException();

            MessageDigest digestor = digestor();

            if(t.tree_id != buf.getInt() ||
                    !Arrays.equals(digest(srcPath, digestor, r.encoder()), readDigest(meta, buf)) ||
                    !Arrays.equals(digest(backupPath, digestor, r.encoder()), readDigest(meta, buf)) 
                    ) {
                throw new ValidationFailedException();
            }

            final int size = buf.getInt();

            this.srcAttrs = new Attr[size];
            this.backupAttrs = new Attr[size];
            int[] dir_count = new int[size];

            while(true) {
                if(!buf.hasRemaining() && IOUtils.read(buf, true, meta) < 0)
                    break;

                readIf(meta, buf, 4);
                int id = buf.getInt();
                dir_count[id] = buf.getInt();

                srcAttrs[id] = readAttrs(meta, buf);
                backupAttrs[id]  = readAttrs(meta, buf);
            }

            AbstractFileImpl[] files = new AbstractFileImpl[size]; 
            buf.clear();

            try(FileChannel names = FileChannel.open(t.namesPath, READ);
                    DataReader d = new DataReader(names, buf);
                    ) {

                d.setChars(r.chars());
                d.setStringBuilder(r.sb());
                d.setDecoder(r.decoder());

                if(d.readInt() != t.tree_id ||
                        !d.readUTF().equals(srcPath.string()) ||
                        !d.readUTF().equals(backupPath.string())
                        ) {
                    throw new ValidationFailedException();
                }

                while(true) {
                    int id = d.readInt();
                    boolean isDir;
                    
                    try {
                        isDir = d.readBoolean(); 
                    } catch (EOFException e) {
                        break;
                    }

                    if(isDir) 
                        files[id] = newDir(id, d.readInt(), dir_count[id], d.readUTF());
                    else 
                        files[id] = newFile(id, d.readInt(), d.readUTF());
                }
            }
            
            for (AbstractFileImpl f : files) {
                if(f == null)
                    continue;
                
                int p = id(f.getParent());
                if(p != -1)
                    addChild(files[p], f);
            }

            this.files = new ArrayWrap<>(files);
        }
    }

    protected abstract void addChild(AbstractFileImpl parent, AbstractFileImpl child);
    protected abstract int id(FileEntity f);
    protected abstract AbstractFileImpl newFile(int id, int parent_id, String filename);
    protected abstract AbstractDirImpl newDir(int id, int parent_id, int child_count, String filename);

    private byte[] readDigest(FileChannel meta, ByteBuffer buf) throws IOException {
        readIf(meta, buf, 4);
        int size = buf.getInt();
        if(size > 200)
            throw new IOException("invalid size: "+size);

        byte[] b = new byte[size];
        for (int i = 0; i < b.length; i++) {
            readIf(meta, buf, 1);
            b[i] = buf.get();
        }
        return b;
    }

    private void readIf(FileChannel fc, ByteBuffer buf, int num) throws IOException {
        if(buf.remaining() < num) {
            IOUtils.compactOrClear(buf);
            IOUtils.read(buf, false, fc);
        }
    }

    public static class ValidationFailedException extends IOException {
        private static final long serialVersionUID = 7433179621779667610L;

        public ValidationFailedException() {
            super();
        }
        public ValidationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
        public ValidationFailedException(String message) {
            super(message);
        }
        public ValidationFailedException(Throwable cause) {
            super(cause);
        }
    }

}
