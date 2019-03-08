package sam.backup.manager.config.json.impl;

import org.json.JSONArray;
import org.json.JSONObject;

import sam.backup.manager.config.api.IFilter;
import sam.backup.manager.config.impl.FilterImpl;
import sam.backup.manager.config.json.impl.JsonConfigManager.JConfig;

abstract class JFilter extends FilterImpl implements Settable {
	protected JConfig config;

	@Override
	public void set(String key, Object value) {
		switch (key) {
			case "name":       this.name = array(value); break;
			case "glob":       this.name = array(value); break;
			case "regex":      this.name = array(value); break;
			case "path":       this.name = array(value); break;
			case "startsWith": this.name = array(value); break;
			case "endsWith":   this.name = array(value); break;
			case "classes":    this.name = array(value); break;
			case "invert":
				if(value != null) 
					this.invert = getFilter((JSONObject) value);
				break;
			default:
				throw new IllegalArgumentException("unknown key: "+key+", value: "+value);
		}
	}

	protected abstract IFilter getFilter(JSONObject value);

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

	public void setConfig(JConfig config) {
		this.config = config;
		JsonConfigManager.setConfig(invert, config);
	}
}

