package sam.backup.manager.config.filter;

import java.util.Map;

import sam.string.StringBuilder2;

public interface HasFilterArrays {
	public Map<String, String[]> getArrays();
	
	default String asString() {
		StringBuilder2 sb = new StringBuilder2();
		
		getArrays().forEach((s,t) -> {
			sb.append(s).append("  ").append('[')
			.appendJoined(", ", t)
			.append("]\n");
		});
		
		return sb.toString();
	}
}
