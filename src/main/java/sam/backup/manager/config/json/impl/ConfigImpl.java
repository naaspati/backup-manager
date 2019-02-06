package sam.backup.manager.config.json.impl;

import static sam.backup.manager.config.json.impl.JsonKeys.BACKUP_CONFIG;
import static sam.backup.manager.config.json.impl.JsonKeys.DISABLE;
import static sam.backup.manager.config.json.impl.JsonKeys.EXCLUDES;
import static sam.backup.manager.config.json.impl.JsonKeys.SOURCE;
import static sam.backup.manager.config.json.impl.JsonKeys.TARGET;
import static sam.backup.manager.config.json.impl.JsonKeys.TARGET_EXCLUDES;
import static sam.backup.manager.config.json.impl.JsonKeys.WALK_CONFIG;
import static sam.backup.manager.config.json.impl.JsonKeys.ZIP_IF;
import static sam.backup.manager.config.json.impl.Utils.getFilter;
import static sam.backup.manager.config.json.impl.Utils.getList;
import static sam.backup.manager.config.json.impl.Utils.set;

import java.nio.file.Path;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import sam.backup.manager.config.WalkConfig;
import sam.backup.manager.config.api.BackupConfig;
import sam.nopkg.Junk;

class ConfigImpl {
	protected final String name;
	protected final List<String> source;
	protected final String target;
	protected final boolean disable;
	
	protected final Filter zip;
	protected final Filter excludes;
	protected final Filter targetExcludes;
	protected final BackupConfigImpl backupConfig;
	protected final WalkConfigImpl walkConfig;
	
	public ConfigImpl(String name, JSONObject json, ConfigImpl global)  {
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
			this.backupConfig = set(json.get(BACKUP_CONFIG), new BackupConfigImpl(global.backupConfig));
			this.walkConfig = set(json.get(WALK_CONFIG), new WalkConfigImpl(global.walkConfig));
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

	public Path resolve(String s) {
		return Junk.notYetImplemented();
		/* FIXME
		 * {
						if(s.charAt(0) == '\\' || s.charAt(0) == '/')
							return config.getSource().resolve(s.substring(1));
						if(s.contains("%source%"))
							s = s.replace("%source%", config.getSource().toString());
						if(s.contains("%target%") && config.getTarget() != null)
							s = s.replace("%target%", config.getTarget().toString());
						return Paths.get(s);
					}
		 */
	}
}
