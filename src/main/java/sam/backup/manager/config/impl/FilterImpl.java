package sam.backup.manager.config.impl;

import static sam.myutils.Checker.isEmpty;
import static sam.myutils.Checker.isNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import sam.backup.manager.Utils;
import sam.backup.manager.config.api.Filter;
import sam.myutils.Checker;
import sam.myutils.System2;
import sam.string.StringUtils;

public abstract class FilterImpl implements Filter {
	public static final String NAME = "name";
	public static final String GLOB = "glob";
	public static final String REGEX = "regex";
	public static final String PATH = "path";
	public static final String STARTS_WITH = "startsWith";
	public static final String ENDS_WITH = "endsWith";
	public static final String CLASSES = "classes";
	public static final String INVERT = "invert";
	
	private transient static final Logger LOGGER = Utils.getLogger(FilterImpl.class);

	protected String[] name, glob, regex, path, startsWith, endsWith, classes;
	protected Filter invert;

	@Override
	public boolean test(Path p) {
		if(invert != null && invert.test(p)){
			LOGGER.debug("INVERT FILTER for: {}", p);
			return false;
		}

		return path(p) ||
				name(p.getFileName()) ||
				startsWith(p) ||
				endsWith(p) || 
				glob(p) ||
				regex(p) ||
				classes(p);
	}

	@Override
	public Map<String, Object> toMap() {
		if(isAlwaysFalse())
			return Collections.emptyMap();

		Map<String, Object> map = new HashMap<>();

		put(NAME, name, map);
		put(GLOB, glob, map);
		put(REGEX, regex, map);
		put(PATH, path, map);
		put(STARTS_WITH, startsWith, map);
		put(ENDS_WITH, endsWith, map);
		put(CLASSES, classes, map);

		if(invert != null) {
			Map<String, Object> map2 = invert.toMap();
			if(map2 != null)
				map.put(INVERT, map2);
		}
		return map;
	}

	protected void put(String title, String[] array, Map<String, Object> map) {
		if(Checker.isNotEmpty(array))
			map.put(title, Collections.unmodifiableList(Arrays.asList(array)));
	}

	private static <E> Set<E> set(String[] array, Function<String, E> mapper) {
		Set<E> set = null;

		for (String s : array) {
			if(Checker.isNotEmptyTrimmed(s)) {
				if(set == null)
					set = new HashSet<>();
				set.add(mapper.apply(s));
			}
		}
		return isEmpty(set) ? Collections.emptySet() : set;
	}
	private Predicate<Path> toPredicate(String[] array, Function<String, Predicate<Path>> mapper) {
		Predicate<Path> p  = null;
		for (String s : array) {
			Predicate<Path> p2 = mapper.apply(s);
			p = p == null ? p2 : p.or(p2);
		}
		return p;
	}
	Predicate<Path> clss;
	@SuppressWarnings("unchecked")
	private boolean classes(Path p) {
		if(isEmpty(classes))
			return false;

		if(isNull(clss)) {
			String path = System2.lookup("custom.filter.dir");
			if(path == null)
				LOGGER.warn("no value set for custom.filter.dir");

			ClassLoader temp = null;
			try {
				temp = path == null ? ClassLoader.getSystemClassLoader() : new URLClassLoader(new URL[] {new File(path).toURI().toURL()}, ClassLoader.getSystemClassLoader());
			} catch (MalformedURLException e1) {
				LOGGER.error("bad value for custom.filter.dir: {}", path);
				System.exit(0);
			}

			ClassLoader loader = temp;

			clss = toPredicate(classes, s -> {
				try {
					return (Predicate<Path>)Class.forName(s,false, loader).newInstance();
				} catch (ClassNotFoundException|ClassCastException | InstantiationException | IllegalAccessException e) {
					LOGGER.error("filter class error: className: {}", s, e);
					System.exit(0);
				}
				return null;
			});
		}
		return clss.test(p);
	}

	private Predicate<Path> endsWiths;
	private boolean endsWith(Path p) {
		if(isEmpty(endsWith))
			return false;
		if(isNull(endsWiths)) {
			endsWiths = toPredicate(endsWith, s -> {
				Path path = Paths.get(s); 
				return x -> x.endsWith(path);
			}); 
		}
		return endsWiths.test(p);
	}

	private Predicate<Path> startsWiths;
	private boolean startsWith(Path p) {
		if(isEmpty(startsWith))
			return false;
		if(isNull(startsWiths)) {
			startsWiths = toPredicate(startsWith, s -> {
				Path path = Paths.get(s); 
				return x -> x.startsWith(path);
			});
		}
		return startsWiths.test(p);
	}

	private Set<Path> paths;
	private boolean path(Path p) {
		if(isEmpty(path))
			return false;

		if(isNull(paths)) 
			paths = set(path, Paths::get);

		return paths.contains(p);
	}

	private Set<Path> names;
	private boolean name(Path filename) {
		if(isEmpty(name))
			return false;
		if(isNull(names))
			names = set(name, Paths::get);

		return names.contains(filename); 
	}

	private Predicate<Path> regexs;

	private boolean regex(Path p) {
		if(isEmpty(regex))
			return false;
		if(isNull(regexs)) {
			regexs = toPredicate(regex, s -> {
				PathMatcher m = fs.getPathMatcher("regex:".concat(s.replace("/", "\\\\")));
				return x -> m.matches(x);
			});
		}
		return regexs.test(p);
	}

	private Predicate<Path> globs;
	private final static FileSystem fs = FileSystems.getDefault();

	private boolean glob(Path p) {
		if(isEmpty(glob))
			return false;

		if(isNull(globs)) {
			globs = toPredicate(glob, s -> {
				PathMatcher rgx =  fs.getPathMatcher("glob:".concat(s));
				return StringUtils.contains(s, '/') ? (x -> rgx.matches(x)) : (x -> rgx.matches(x.getFileName())); 
			}); 
		}
		return globs.test(p);
	}
	public boolean isAlwaysFalse() {
		return invert == null && Checker.allMatch(Checker::isEmpty, name, glob, regex, path, startsWith, endsWith, classes);
	}
}
