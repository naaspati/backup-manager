package sam.backup.manager.config;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import sam.myutils.stringutils.StringUtils;

public class Exclude {
	private Set<Path> fullPaths, names;
	private Predicate<Path> partialPaths, globs, nameGlobs;
	private boolean noExclude = false;

	public Exclude(String[] excludes) {
		if(excludes == null || excludes.length == 0) {
			noExclude = true;
			return;
		}
		
		FileSystem fs = null;

		for (String s : excludes) {
			if(StringUtils.contains(s,'*')) {
				fs = fs != null ? fs : FileSystems.getDefault(); 
				PathMatcher pm = fs.getPathMatcher("glob:"+s);
				if(StringUtils.contains(s, '/'))
					globs = predicate(globs, pm);
				else
					nameGlobs = predicate(nameGlobs, pm);
			}
			else {
				Path p = Paths.get(s);
				if(p.getRoot() == null && p.getNameCount() == 1)
					names = add(names, p);
				else if(p.getRoot() != null)
					fullPaths = add(fullPaths, p);
				else {
					if(partialPaths == null) partialPaths = p2 -> p2.endsWith(p);
					else partialPaths = partialPaths.or(p2 -> p2.endsWith(p));
				}
			}
		}
	}

	private Predicate<Path> predicate(Predicate<Path> existing, PathMatcher pm) {
		if(existing == null)
			return p -> pm.matches(p);

			return existing.or(p -> pm.matches(p));
	}
	private Set<Path> add(Set<Path> list, Path s) {
		if(list == null) list = new HashSet<>();
		list.add(s);
		return list;
	}

	public boolean exclude(Path p) {
		if(noExclude) return false;
		
		if((fullPaths != null && fullPaths.contains(p)) || (names != null && names.contains(p.getFileName())))
			return true;
		if(partialPaths != null && partialPaths.test(p))
			return true;
		if(nameGlobs != null && nameGlobs.test(p.getFileName()))
			return true;
		if(globs != null && globs.test(p))
			return true;

		return false;
	}
}
