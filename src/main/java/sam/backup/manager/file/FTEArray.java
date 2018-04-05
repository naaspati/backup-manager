package sam.backup.manager.file;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * FileTreeEntityArray
 * @author Sameer
 *
 */
public class FTEArray implements Iterable<FileTreeEntity> {
	private FileTreeEntity[] array; 
	private int index = 0;

	public FTEArray(int size) {
		array = new FileTreeEntity[size];
	}
	public FTEArray() {
		this(30);
	}

	public int size() {
		return index;
	}
	@Override
	public Iterator<FileTreeEntity> iterator() {
		return new Iterator<FileTreeEntity>() {
			int n = 0;

			@Override
			public FileTreeEntity next() {
				return array[n++];
			}

			@Override
			public boolean hasNext() {
				return n < index;
			}
		};
	}
	public void add(FileTreeEntity f) {
		if(index == array.length)
			array = Arrays.copyOf(array, index * 2);

		array[index++] = f;  
	}
	public FileTreeEntity get(int index) {
		return array[index];
	}
	public boolean isEmpty() {
		return index == 0;
	}
	public void sort(Comparator<FileTreeEntity> comparator) {
		Arrays.sort(array, 0, index, comparator);
	}
	public Stream<FileTreeEntity> stream() {
		return Arrays.stream(array, 0, index);
	}
	public boolean remove(FileTreeEntity ft) {
		int n = indexOf(ft);
		if(n < 0)
			return false;
		
		while(n < array.length - 1)
			array[n] = array[++n];
		
		this.index--;
		return true;
	}
	private int indexOf(FileTreeEntity ft) {
		if(isEmpty())
			return -1;
		
		for (int i = 0; i < index; i++) {
			if(ft == array[i])
				return i;
		}
		
		return -1;
	}
}
