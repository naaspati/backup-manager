package sam.backup.manager.file;

import java.io.IOException;

import sam.functions.IOExceptionConsumer;
import sam.myutils.Checker;

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
    public void set(int index, E e){
        if(index >= size)
            throw new IndexOutOfBoundsException("id("+index+") >= size("+size+")");
        if(index < data.length)
            data[index] = e;
        else 
            data[index - data.length] = e;

    }
    public boolean isModified() {
        return data.length != size;
    }

    public int newSize() {
        return new_data == null ? 0 : new_data.length;
    }

    public void forEachNew(IOExceptionConsumer<E> consumer) throws IOException {
        apply(new_data, consumer); 
    }

    private void apply(E[] data, IOExceptionConsumer<E> consumer) throws IOException {
        if(Checker.isEmpty(data))
            return;

        for (E e : data) {
            if(e != null)
                consumer.accept(e);
        }
    }

    public void forEach(IOExceptionConsumer<E> consumer) throws IOException {
        apply(data, consumer);
        apply(new_data, consumer);
    }
}
