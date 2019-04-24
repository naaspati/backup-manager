package sam.backup.manager.config.dummy.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import sam.backup.manager.config.api.BackupConfig;
import sam.backup.manager.config.api.Config;
import sam.backup.manager.config.api.ConfigManager;
import sam.backup.manager.config.api.ConfigType;
import sam.backup.manager.config.api.FileTreeMeta;
import sam.backup.manager.config.api.Filter;
import sam.backup.manager.config.api.WalkConfig;
import sam.backup.manager.config.impl.ConfigImpl;
import sam.backup.manager.config.impl.PathWrap;
import sam.backup.manager.file.api.FileTree;
import sam.backup.manager.file.api.FileTreeManager;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;

class DummyConfigManager implements ConfigManager {
    private static final EnsureSingleton cm_singleton = new EnsureSingleton();

    final List<Config> backup;
    final List<Config> list; 

    public DummyConfigManager() {
        cm_singleton.init();

        int backupsize = Integer.parseInt(System.getProperty("CM.backup.size", "10"));
        int listsize = Integer.parseInt(System.getProperty("CM.list.size", "10"));

        this.backup = generate(ConfigType.BACKUP, backupsize);
        this.list = generate(ConfigType.LIST, listsize);
    }

    private List<Config> generate(ConfigType type, int size) {
        if(size < 0)
            throw new IllegalArgumentException("negative size("+size+"), for: "+type);
        if(size == 0)
            return Collections.emptyList();

        Config[] configs = new Config[size];

        for (int i = 0; i < size; i++) 
            configs[i] = new DumConfig(type+"-"+i, type, ftms(type, i), false, filter("zip",type, i), filter("excludes", type, i), filter("targetExcludes", type, i), backupConfig(type, i), walkConfig(type, i));

        return Collections.unmodifiableList(Arrays.asList(configs));
    }

    private WalkConfig walkConfig(ConfigType type, int i) {
        return new WalkConfig() {

            @Override
            public boolean walkBackup() {
                return false;
            }
            @Override
            public boolean skipFiles() {
                return false;
            }
            @Override
            public boolean skipDirNotModified() {
                return false;
            }
            @Override
            public int getDepth() {
                return Integer.MAX_VALUE;
            }
        };
    }

    private BackupConfig backupConfig(ConfigType type, int i) {
        return new BackupConfig() {
            @Override
            public boolean hardSync() {
                return false;
            }

            @Override
            public boolean checkModified() {
                return true;
            }
        };
    }

    private Filter filter(String string, ConfigType type, int i) {
        return null;
    }

    private List<FileTreeMeta> ftms(ConfigType type, int i) {
        return Arrays.asList(new FTM(new PathWrap2("type/1/"+i), new PathWrap2("type/2/"+i)));
    }

    public static class PathWrap2 extends PathWrap {
        public PathWrap2(String s) {
            super(s);
        }
        @Override
        public boolean exists() {
            return true;
        }
    }

    @Override
    public Collection<Config> get(ConfigType type) {
        return Objects.requireNonNull(type) == ConfigType.BACKUP ? backup : list;
    }

    @Override
    public Long getBackupLastPerformed(ConfigType type, Config config) {
        return Junk.notYetImplemented();
    }

    @Override
    public void putBackupLastPerformed(ConfigType type, Config config, long time) {
        Junk.notYetImplemented();
    }

    private static class FTM implements FileTreeMeta {
        final PathWrap s,t;

        public FTM(PathWrap s, PathWrap t) {
            this.s = s;
            this.t = t;
        }

        @Override
        public PathWrap getSource() {
            return s;
        }

        @Override
        public PathWrap getTarget() {
            return t;
        }

        @Override
        public FileTree getFileTree() {
            return Junk.notYetImplemented();
        }

        @Override
        public long getLastModified() {
            return System.currentTimeMillis();
        }

        @Override
        public FileTree loadFiletree(FileTreeManager manager, boolean createNewIfNotExists) throws IOException {
            return Junk.notYetImplemented();
        }
    }

    private static class DumConfig extends ConfigImpl {
        public DumConfig(String name, ConfigType type, List<FileTreeMeta> ftms, boolean disable, Filter zip, Filter excludes, Filter targetExcludes, BackupConfig backupConfig, WalkConfig walkConfig) {
            super(name, type, ftms, disable, zip, excludes, targetExcludes, backupConfig, walkConfig);
        }
    }
}
