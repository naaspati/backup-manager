package sam.backup.manager.file.db;

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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (modifiedTime ^ (modifiedTime >>> 32));
		result = prime * result + (int) (size ^ (size >>> 32));
		return result;
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
		return modifiedTime == other.modifiedTime && size == other.size; 
	}

}
