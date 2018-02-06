package sam.backup.manager.file;

import java.nio.file.attribute.BasicFileAttributes;

public class AboutFile {
	public final long modifiedTime;
	public final long size;
	
	public AboutFile(BasicFileAttributes attrs) {
		modifiedTime = attrs.lastModifiedTime().toMillis();
		size = attrs.size();
	}
	
	public AboutFile(long modifiedTime, long size) {
		this.modifiedTime = modifiedTime;
		this.size = size;
	}
	public long getModifiedTime() {
		return modifiedTime;
	}
	public long getSize() {
		return size;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AboutFile [modifiedTime=");
		builder.append(modifiedTime);
		builder.append(", size=");
		builder.append(size);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AboutFile other = (AboutFile) obj;
		if (modifiedTime != other.modifiedTime)
			return false;
		if (size != other.size)
			return false;
		return true;
	}
	
}
