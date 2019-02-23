package sam.backup.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

@SuppressWarnings({"rawtypes", "unchecked"})
interface AppInitHelper {
	
	static Map<Class, Class> getClassesMapping(Class<App> cls, String name, Logger logger) throws ClassNotFoundException, IOException{
		InputStream is = cls.getResourceAsStream(name);
		if(is == null)
			throw new IOException("no resource found for: "+name);

		Map<Class, Class> map = new HashMap<>();
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));) {
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				String s = line.trim();
				
				String t = s.trim();
				if(t.isEmpty() || t.charAt(0) == '#')
					continue;
				
				int n = s.indexOf('=');
				if(n < 0)
					logger.warn("bad line: \"{}\"", line);

				Class sup = Class.forName(s.substring(0, n).trim());
				Class child = Class.forName(s.substring(n + 1).trim()); 
				map.put(sup, child);
				
				/*
				 * if(!sup.isAssignableFrom(child))
					throw new ClassNotFoundException(String.format("!%s.isAssignableFrom(%s)", sup, child));
				 */
				
			}
		}
		
		return map;
	}

	static <E> E instance(Map<Class, Class> map, Class required,  Class defaultImplementation, Consumer<String> logConsumer) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class implementation = map.get(required);
		if(implementation == null) {
			logConsumer.accept("no implementation specified for: "+required);
			if(defaultImplementation == null)
				throw new ClassNotFoundException("no implementation specified for: "+required);
			else 
				return (E) defaultImplementation.newInstance();
		} else {
			logConsumer.accept("implementation found for: "+required+",  is: "+implementation);
			return (E) implementation.newInstance();
		}
	}

}
