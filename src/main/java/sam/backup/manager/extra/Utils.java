package sam.backup.manager.extra;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Utils {
	private Utils() {}
	
	public static String bytesToString(long bytes) {
		if(bytes < 1048576)
			return bytesToString(bytes, 1024) + "KB";
		if(bytes < 1073741824)
			return bytesToString(bytes, 1048576) + "MB";
		else
			return bytesToString(bytes, 1073741824) + "GB";
		
	}
	
	private static String bytesToString(long bytes, long divisor) {
		double d = divide(bytes, divisor);
		if(d == (int)d) return String.valueOf((int)d);
		else return String.valueOf(d);
	}

	private final static StringBuilder sb = new StringBuilder();
	public static String millisToString(long millis) {
		return durationToString(Duration.ofMillis(millis));
	}
	public static String durationToString(Duration d) {
		synchronized (sb) {
			sb.setLength(0);
			
			char[] chars = d.toString().toCharArray();
			for (int i = 2; i < chars.length; i++) {
				char c = chars[i];
				switch (c) {
				case 'H':
					sb.append("hours ");
					break;
				case 'M':
					sb.append("min ");
					break;
				case 'S':
					sb.append("sec");
					break;
				case '.':
					sb.append("sec");
					return sb.toString();
				default:
					sb.append(c);
					break;
				}
			}
			return sb.toString();
		}
	}
	public static double divide(long dividend, long divisor) {
		if(divisor == 0 || dividend == 0)
			return 0;
		return (dividend*100/divisor)/100D;
	}
	public static String millsToTimeString(Long lastUpdated) {
		return lastUpdated == null ? "--" : LocalDateTime.ofInstant(Instant.ofEpochMilli(lastUpdated), ZoneOffset.of("+05:30")).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

}
