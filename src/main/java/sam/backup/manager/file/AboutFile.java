package sam.backup.manager.file;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class AboutFile {
	private final FileTime modifiedTime;
	private final long size;
	
	public AboutFile(BasicFileAttributes attrs) {
		modifiedTime = attrs.lastModifiedTime();
		size = attrs.size();
	}
	
	public AboutFile(FileTime modifiedTime, long size) {
		this.modifiedTime = modifiedTime;
		this.size = size;
	}
	public FileTime getModifiedTime() {
		return modifiedTime;
	}
	public long getSize() {
		return size;
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
		if (modifiedTime == null) {
			if (other.modifiedTime != null)
				return false;
		} else if (!modifiedTime.equals(other.modifiedTime))
			return false;
		if (size != other.size)
			return false;
		return true;
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
	
	
}
