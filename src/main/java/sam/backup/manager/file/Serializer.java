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
import java.util.BitSet;

import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.AbstractDir;
import sam.backup.manager.file.api.AbstractFileEntity;
import sam.backup.manager.file.api.Attr;
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

    ArrayWrap<AbstractFileEntity> files;
    public TreePaths t;
    public PathWrap srcPath, backupPath;
    public Attr[] srcAttrs, new_srcAttrs;
    public Attr[] backupAttrs, new_backupAttrs;
    public BitSet attrMod;

    public void save() throws IOException, NoSuchAlgorithmException {
        Checker.requireNonNull("files, t, srcPath, backupPath, srcAttrs, backupAttrs, attrMod", files, t, srcPath, backupPath, srcAttrs, backupAttrs, attrMod);

        if(!files.isModified())
            return;

        try(Resources r = Resources.get()) {
            Path nm = null;
            if(files.newSize() != 0 || files.size() < 2)
                nm = writeNames(files, r);

            Path mt = null;
            if(attrMod.cardinality() != 0)
                mt = writeMeta(files, r);

            if(nm != null)
                Files.move(nm, t.namesPath, REPLACE_EXISTING);
            if(mt != null)
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

    private Path writeMeta(ArrayWrap<AbstractFileEntity> files, Resources r) throws IOException, NoSuchAlgorithmException {
        MessageDigest digestor = digestor();

        /**
         * TODO 
         * decide: 
         *   - truncate whole file rewrite all attrs (current method)
         *   - append changed attrs
         *   
         * possibly in future:
         *   - check number of changes in attr and decide append new changes or rewrite whole data  
         */
        return write(t.metaPath, TRUNCATE_EXISTING, fc -> {
            ByteBuffer buf = r.buffer();
            buf.clear();
            CharsetEncoder encoder = r.encoder();

            buf.putInt(t.tree_id);
            write_digest(fc, buf, srcPath, digestor, encoder);
            write_digest(fc, buf, backupPath, digestor, encoder);

            writeIf(fc, buf, 4);
            buf.putInt(files.size());

            final int BYTES = Integer.BYTES * 2 + Attr.BYTES * 2;
            
            int size = files.size();
            for (int i = 0; i < size; i++) {
                Attr s = get(srcAttrs, new_srcAttrs, i);
                Attr b = get(backupAttrs, new_backupAttrs, i);
                
                if(s == null || b == null)
                    continue;
                
                writeIf(fc, buf, BYTES);
                AbstractFileEntity f = files.get(i);

                buf.putInt(i);
                buf.putInt(f.isDirectory() ? ((Dir)f).childrenCount() : -1);
                writeAttrs(s, buf);
                writeAttrs(b, buf);
            }

            writeIf(fc, buf, 4);
            buf.putInt(Integer.MAX_VALUE);
            IOUtils.write(buf, fc, true);
        });
    }

    private Attr get(Attr[] a, Attr[] b, int n) {
        return n < a.length ? a[n] : b[n - a.length];
    }

    private void writeIf(FileChannel fc, ByteBuffer buf, int num) throws IOException {
        if(buf.remaining() < num)
            IOUtils.write(buf, fc, true);
    }

    private void write_digest(FileChannel fc, ByteBuffer buf,  PathWrap p, MessageDigest d, CharsetEncoder e) throws IOException {
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
    
    private void writeAttrs(Attr a, ByteBuffer buf) {
        if(a == null) 
            buf.putLong(NULL_ATTR);
        else if(a == FileTree.EMPTY_ATTR)
            buf.putLong(EMPTY_ATTR);
        else {
            buf.putLong(a.lastModified);
            buf.putLong(a.size);    
        }
    }

    private Attr readAttr(FileChannel fc, ByteBuffer buf) throws IOException {
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

    private Path writeNames(ArrayWrap<AbstractFileEntity> files, Resources r) throws IOException {
        boolean rewrite = files.size() < 2;

        return write(t.namesPath, rewrite ? TRUNCATE_EXISTING : APPEND, fc -> {
            try(DataWriter d = new DataWriter(fc, r.buffer())) {
                d.setEncoder(r.encoder());

                IOExceptionConsumer<AbstractFileEntity> w = f -> {
                    d.writeBoolean(f.isDirectory());
                    d.writeInt(id(f));
                    d.writeInt(id(f.getParent()));
                    d.writeUTF(f.filename);
                };

                if(rewrite) {
                    d.writeInt(t.tree_id);
                    d.writeUTF(srcPath.string());
                    d.writeUTF(backupPath.string());

                    files.forEach(w);
                } else {
                    files.forEachNew(w);    
                }
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
                readIf(meta, buf, 4);
                int id = buf.getInt();
                if(id == Integer.MAX_VALUE)
                    break;

                dir_count[id] = buf.getInt();

                srcAttrs[id] = readAttr(meta, buf);
                backupAttrs[id]  = readAttr(meta, buf);
            }

            AbstractFileEntity[] files = new AbstractFileEntity[size]; 
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

            for (AbstractFileEntity f : files) {
                if(f == null)
                    continue;

                int p = id(f.getParent());
                if(p != -1)
                    addChild(files[p], f);
            }

            this.files = new ArrayWrap<>(files);
        }
    }

    protected abstract void addChild(AbstractFileEntity parent, AbstractFileEntity child);
    protected abstract int id(FileEntity f);
    protected abstract AbstractFileEntity newFile(int id, int parent_id, String filename);
    protected abstract AbstractDir newDir(int id, int parent_id, int child_count, String filename);

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
