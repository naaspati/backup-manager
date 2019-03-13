package sam.backup.manager.file;

import sam.nopkg.Junk;

class ArrayWrap<E> {
	private int size;
	private final E[] data;
	private E[] new_data;

	public ArrayWrap(E[] data) {
		this.data = data;
		this.size = data.length;
	}

	public int oldSize() {
		return data.length;
	}
	public int size() {
		return size;
	}

	public E get(int index) {
		return index >= size ? null : data[index < data.length ? index : index - data.length];
	}
	public void set(int id, E e){
		Junk.notYetImplemented(); //FIXME
	}
	public boolean isModified() {
		return data.length != size;
	}
}
