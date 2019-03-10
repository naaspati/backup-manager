package sam.backup.manager.config.json.impl;

import java.util.function.BiFunction;

import org.json.JSONArray;
import org.json.JSONObject;

import sam.backup.manager.config.api.Filter;
import sam.backup.manager.config.impl.FilterImpl;
import sam.backup.manager.config.json.impl.JsonConfigManager.Vars;
import sam.myutils.Checker;

class JFilter extends FilterImpl implements Settable {
	public static String[] validKeys() {
		return new String[]{NAME, GLOB, REGEX, PATH, STARTS_WITH, ENDS_WITH, CLASSES, INVERT};
	}
	
	private Vars vars;
	private BiFunction<JSONObject, Vars, Filter> filterMaker;

	public JFilter(Vars vars, BiFunction<JSONObject, Vars, Filter> filterMaker) {
		this.vars = vars;
		this.filterMaker = filterMaker;
	}

	@Override
	public void set(String key, Object value) {
		if(INVERT.equals(key)) {
			if(value != null) 
				this.invert = filterMaker.apply((JSONObject) value, vars);
		} else {
			String[] array = array(value);
			
			if(Checker.isEmpty(array))
				return;
			
			switch (key) {
				case NAME:       this.name        = array; break;
				case GLOB:       this.glob        = array; break;
				case REGEX:      this.regex       = array; break;
				case PATH:       this.path        = array; break;
				case STARTS_WITH: this.startsWith  = array; break;
				case ENDS_WITH:   this.endsWith    = array; break;
				case CLASSES:    this.classes     = array; break;
				default:
					throw new IllegalArgumentException("unknown key: "+key+", value: "+value);
			}
		}
	}

	private String[] array(Object value) {
		if(value == null)
			return null;

		JSONArray array = (JSONArray) value;
		if(array.length() == 0)
			return null;

		String[] str = new String[array.length()];
		for (int i = 0; i < str.length; i++) 
			str[i] = array.getString(i);

		return str;
	}
	void init() {
		for (String[] sar : new String[][]{name, glob, regex, path, startsWith, endsWith, classes}) {
			if(Checker.isNotEmpty(sar)) {
				for (int i = 0; i < sar.length; i++) 
					sar[i] = vars.resolve(sar[i]);
			}
		}
		
		filterMaker = null;
	}
}
