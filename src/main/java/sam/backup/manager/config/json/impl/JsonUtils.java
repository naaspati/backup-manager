package sam.backup.manager.config.json.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import sam.nopkg.Junk;

interface JsonUtils {
	public static <T> T either(T t1, T t2, T defaultValue) {
		if(t1 == null && t2 == null)
			return defaultValue;
		return t1 != null ? t1 : t2;
	}

	public static List<String> getList(Object obj, boolean unmodifiable) {
		if(obj == null)
			return Collections.emptyList();
		if(obj.getClass() == String.class)
			return Collections.singletonList((String)obj);
		if(obj instanceof JSONArray) {
			JSONArray array = ((JSONArray) obj);
			if(array.isEmpty())
				return Collections.emptyList();
			if(array.length() == 1)
				return Collections.singletonList(array.getString(0));

			String[] str = new String[array.length()];
			for (int i = 0; i < array.length(); i++) 
				str[i] = array.getString(i);

			List<String> list = Arrays.asList(str);

			return unmodifiable ? Collections.unmodifiableList(list) : list;
		}
		throw new IllegalArgumentException("bad type: "+obj);
	}

	public static FilterImpl getFilter(Object obj, String jsonKey) {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
	
	@SuppressWarnings("unchecked")
	public static <E> E set(Object jsonObj, Settable settable) {
		if(jsonObj != null) {
			JSONObject json = (JSONObject) jsonObj;

			for (String s : json.keySet()) 
				settable.set(s, json.get(s));
		}
		return (E) settable;
	}
}