package sam.backup.manager.config;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import sam.backup.manager.config.filter.Filter;
import static sam.backup.manager.config.ConfigKeys.*;
import static sam.backup.manager.config.ConfigUtils.*;

public class Config {
	protected final String name;
	protected final List<String> source;
	protected final String target;
	protected final boolean disable;
	
	protected final Filter zip;
	protected final Filter excludes;
	protected final Filter targetExcludes;
	protected final BackupConfig backupConfig;
	protected final WalkConfig walkConfig;
	
	public Config(String name, JSONObject json, Config global) {
		try {
			this.name = name;
			this.source = getList(json.opt(SOURCE), true);
			if(this.source.isEmpty())
				throw new JSONException("source not found in json");
			
			this.target = json.getString(TARGET);
			this.disable = json.optBoolean(DISABLE, false);
			this.zip = getFilter(json.opt(ZIP_IF), ZIP_IF);
			this.excludes = getFilter(json.opt(EXCLUDES), EXCLUDES);
			this.targetExcludes = getFilter(json.opt(TARGET_EXCLUDES), TARGET_EXCLUDES);
			this.backupConfig = set(json.get(BACKUP_CONFIG), new BackupConfig(global.backupConfig));
			this.walkConfig = set(json.get(WALK_CONFIG), new WalkConfig(global.walkConfig));
		} catch (Exception e) {
			throw new JSONException(e.getMessage()+"\n"+json, e);
		}
	}

	public String getName() { return name; }
	public List<String> getSource() { return source; }
	public String getTarget() { return target; }
	public boolean isDisable() { return disable; }
	public Filter getZip() { return zip; }
	public Filter getExcludes() { return excludes; }
	public Filter getTargetExcludes() { return targetExcludes; }
	public BackupConfig getBackupConfig() { return backupConfig; }
	public WalkConfig getWalkConfig() { return walkConfig; }
}
