package sam.backup.manager.file.db;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.Consumer;

import sam.collection.Iterables;
import sam.collection.Iterators;

abstract class List2<E extends FileImpl> implements Iterable<E>  {
	private final E[] fixed;
	private final int fixed_len;
	private E[] current;
	private int modCount;
	private int size; 
	private int index; // index can be greater than size

	List2(E[] fixed) {
		this.fixed = fixed;
		this.index = fixed.length;
		this.size = index;
		this.fixed_len = index;
		modCount++;
	}
	/**
	 * estimated size
	 * @return
	 */
	public final int size() {
		return size;
	}

	protected final int nextId() {
		return index++;	
	}
	protected final E add(E item) {
		int id = item.getId();
		size++;

		if(current == null || current.length < id+1) {
			E[] old = current;
			current = newArray((id == 0 ? 5 : id) * 2);
			if(old != null)
				System.arraycopy(old, 0, current, 0, old.length);
		}
		current[id - fixed_len] = item;
		modCount++;
		return item;
	}
	Iterable<E> iterableOfCurrent() {
		return iterable(current, index - fixed_len);
	}
	protected void cm(int m) {
		if(m != modCount)
			throw new ConcurrentModificationException();
	}
	private Iterable<E> iterable(E[] array, int size) {
		if(array == null || array.length == 0)
			return Iterables.empty();

		return Iterables.of(iter(array, size));
	}

	private Iterator<E> iter(E[] array, int size) {
		int m = modCount;

		return new Iterator<E>() {
			private E e;
			int index = 0;
			{
				next0();
			}

			@Override
			public boolean hasNext() {
				return e != null;
			}
			@Override
			public E next() {
				cm(m);
				E f = e;
				next0();
				return f;
			}
			protected void next0() {
				while(true) {
					if(index >= array.length || index >= size){
						e = null;
						return;
					}
					e = array[index++];
					if(e != null)
						break;
				}
			}
		};
	}
	@Override
	public Iterator<E> iterator() {
		if(index == 0 && fixed.length == 0) return Iterators.empty();

		return new Iterator<E>() {
			Iterator<E> iter1 = iter(current, index);
			boolean second = false;

			@Override
			public boolean hasNext() {
				if(!iter1.hasNext()){
					if(!second) {
						second = true;
						iter1 = iter(fixed, fixed.length);
					}
				}
				return iter1.hasNext();
			}
			@Override
			public E next() {
				return iter1.next();
			}
		};
	}
	@Override
	public void forEach(Consumer<? super E> action) {
		int m = modCount;

		if(index != 0){
			int size = index;
			for (int i = 0; i < size; i++) {
				if(current[i] != null) {
					cm(m);
					action.accept(current[i]);
				}
			}
		}
		if(fixed.length == 0) return ;

		for (E e : fixed) {
			if(e != null) {
				cm(m);
				action.accept(e);
			}
		}
	}
	protected abstract E[] newArray(int size);
}
