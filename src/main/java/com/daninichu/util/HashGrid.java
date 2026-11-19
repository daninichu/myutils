package com.daninichu.util;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A hash-based Grid backed by a custom open-addressing hash table using
 * Robin Hood hashing with linear probing and backward-shift deletion.
 * <p>
 * Keys are stored as packed {@code long}s (x in the high 32 bits, y in the low 32 bits),
 * avoiding {@link Point} allocation on lookups.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid<E> extends AbstractGrid<E> implements Grid<E>{
    private int[] distances;
    private long[] keys;
    private Object[] values;

    private int size, threshold, mask;
    private final float loadFactor;

    /**
     * Constructs an empty {@code HashGrid} with a default initial capacity of 16
     * and a default load factor of 0.9.
     */
    public HashGrid(){
        this.loadFactor = 0.9f;
        init(16);
    }

    /**
     * Constructs an empty {@code HashGrid} with the given initial capacity
     * and a default load factor of 0.9.
     * @param initialCapacity The number of entries this grid can initially hold.
     * @throws IllegalArgumentException if {@code initialCapacity} is less than 2.
     */
    public HashGrid(int initialCapacity){
        this(initialCapacity, 0.9f);
    }

    /**
     * Constructs an empty {@code HashGrid} with the given initial capacity and load factor.
     * @param initialCapacity The number of entries this grid can initially hold.
     * @param loadFactor The fraction of the capacity that may be occupied before a resize occurs.
     * @throws IllegalArgumentException if {@code initialCapacity} is less than 2,
     *         or if {@code loadFactor} does not satisfy {@code 0 < loadFactor && loadFactor < 1}.
     */
    public HashGrid(int initialCapacity, float loadFactor){
        if(initialCapacity < 2)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if(!(0 < loadFactor && loadFactor < 1))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        init(Integer.highestOneBit(initialCapacity - 1) << 1);
    }

    /**
     * Constructs a new {@code HashGrid} with the same entries as {@code grid}
     * with a default load factor of 0.9 and an initial capacity sufficient to
     * hold the entries of {@code grid}.
     * @param grid The grid to copy from.
     * @throws NullPointerException if {@code grid} is null.
     */
    public HashGrid(HashGrid<? extends E> grid){
        this.loadFactor = 0.9f;
        this.size = grid.size;

        int[] distances = grid.distances;
        long[] keys = grid.keys;
        Object[] values = grid.values;

        init(distances.length);
        for(int i = 0, mask = this.mask; i <= mask; i++){
            if(distances[i] != 0){
                long key = keys[i];
                insertAt(hash(key) & mask, key, values[i]);
            }
        }
    }

    private void init(int capacity){
        distances = new int[capacity];
        keys = new long[capacity];
        values = new Object[capacity];
        threshold = (int) (capacity * loadFactor);
        mask = capacity - 1;
    }

    private static long pack(int x, int y){
        return (long) x << 32 | y & 0xFFFFFFFFL;
    }

    private static int unpackX(long key){
        return (int) (key >>> 32);
    }

    private static int unpackY(long key){
        return (int) key;
    }

    private static int hash(long key){
        key *= 0xff57afd7ed558ccdL;
        return (int) (key ^ (key >>> 32));
    }

    /**
     * Returns the slot of {@code key} if present, or {@code ~slot} (a negative value) if absent.
     * The insert slot is the first slot that is empty or holds an entry
     * closer to its home than {@code key} would be at that slot.
     */
    private int findSlot(long key){
        int[] distances = this.distances;
        long[] keys = this.keys;

        int mask = this.mask;
        int i = hash(key) & mask;
        int d = 1;
        while(true){
            if(distances[i] < d)
                return ~i;
            if(keys[i] == key)
                return i;
            i = (i + 1) & mask;
            d++;
        }
    }

    /**
     * Inserts an entry known to be absent, starting at {@code i}.
     * Entries that are closer to their home than the
     * carried entry are displaced forward ("steal from the rich").
     * Does not check the threshold or modify {@code size}.
     */
    private void insertAt(int i, long key, Object value){
        int[] distances = this.distances;
        long[] keys = this.keys;
        Object[] values = this.values;

        int mask = this.mask;
        int d = ((i - hash(key)) & mask) + 1;
        while(distances[i] != 0){
            int residentDist = distances[i];
            if(residentDist < d){
                long tmpKey = keys[i];
                Object tmpValue = values[i];
                distances[i] = d;
                keys[i] = key;
                values[i] = value;
                d = residentDist;
                key = tmpKey;
                value = tmpValue;
            }
            i = (i + 1) & mask;
            d++;
        }
        distances[i] = d;
        keys[i] = key;
        values[i] = value;
    }

    private void insertAndIncrement(int i, long key, Object value){
        if(size >= threshold){
            int newCapacity = distances.length << 1;
            resize(newCapacity);
            i = hash(key) & mask;
        }
        insertAt(i, key, value);
        size++;
    }

    private E rawPut(long key, Object value){
        int i = findSlot(key);
        if(i >= 0){
            E oldValue = (E) values[i];
            values[i] = value;
            return oldValue;
        }
        insertAndIncrement(~i, key, value);
        return null;
    }

    private E rawRemove(long key){
        int i = findSlot(key);
        if(i < 0)
            return null;
        E oldValue = (E) values[i];
        shiftDelete(i);
        return oldValue;
    }

    private void shiftDelete(int deleted){
        int[] distances = this.distances;
        long[] keys = this.keys;
        Object[] values = this.values;

        int mask = this.mask;
        int next = (deleted + 1) & mask;
        while(distances[next] > 1){
            distances[deleted] = distances[next] - 1;
            keys[deleted] = keys[next];
            values[deleted] = values[next];
            deleted = next;
            next = (next + 1) & mask;
        }
        distances[deleted] = 0;
        values[deleted] = null;
        size--;
    }

    private void resize(int newCapacity){
        if(newCapacity < 0)
            throw new OutOfMemoryError("Capacity cannot grow any further");
        int[] distances = this.distances;
        long[] keys = this.keys;
        Object[] values = this.values;

        int oldCapacity = keys.length;
        init(newCapacity);
        if(size != 0){
            int mask = this.mask;
            for(int i = 0; i < oldCapacity; i++){
                if(distances[i] != 0){
                    long key = keys[i];
                    insertAt(hash(key) & mask, key, values[i]);
                }
            }
        }
    }

    /**
     * Increases the table capacity of this {@code HashGrid}, if necessary,
     * to ensure that it can hold at least {@code minCapacity} entries.
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity){
        if(minCapacity > threshold)
            resize(Integer.highestOneBit((int) Math.ceil(minCapacity / loadFactor) - 1) << 1);
    }

    public int size(){
        return size;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    @Override
    public E get(int x, int y){
        int i = findSlot(pack(x, y));
        return i >= 0? (E) values[i] : null;
    }

    @Override
    public E set(int x, int y, E e){
        return rawPut(pack(x, y), e);
    }

    @Override
    public E getOrSet(int x, int y, E e){
        long key = pack(x, y);
        int i = findSlot(key);
        if(i >= 0)
            return (E) values[i];
        insertAndIncrement(~i, key, e);
        return null;
    }

    @Override
    public E compute(int x, int y, Function<? super E, ? extends E> function){
        long key = pack(x, y);
        int i = findSlot(key);
        E e;
        if(i >= 0){
            if((e = function.apply((E) values[i])) == null)
                shiftDelete(i);
            else
                values[i] = e;
        } else if((e = function.apply(null)) != null)
            insertAndIncrement(~i, key, e);
        return e;
    }

    @Override
    public E computeIfAbsent(int x, int y, Supplier<? extends E> function){
        long key = pack(x, y);
        int i = findSlot(key);
        if(i >= 0)
            return (E) values[i];
        E e = function.get();
        if(e != null)
            insertAndIncrement(~i, key, e);
        return e;
    }

    @Override
    public E removePoint(int x, int y){
        return rawRemove(pack(x, y));
    }

    @Override
    public boolean containsPoint(int x, int y){
        return findSlot(pack(x, y)) >= 0;
    }

    @Override
    public boolean removeValue(E e){
        int[] distances = this.distances;
        Object[] values = this.values;

        if(e == null){
            for(int i = 0, n = distances.length; i < n; i++){
                if(distances[i] != 0 && values[i] == null){
                    shiftDelete(i);
                    return true;
                }
            }
        } else{
            for(int i = 0, n = distances.length; i < n; i++){
                if(distances[i] != 0 && e.equals(values[i])){
                    shiftDelete(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(E e){
        int[] distances = this.distances;
        Object[] values = this.values;

        if(e == null){
            for(int i = 0, n = distances.length; i < n; i++)
                if(distances[i] != 0 && values[i] == null)
                    return true;
        } else{
            for(int i = 0, n = distances.length; i < n; i++)
                if(distances[i] != 0 && e.equals(values[i]))
                    return true;
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        int[] distances = this.distances;
        Object[] values = this.values;

        if(e == null){
            for(int i = 0, n = distances.length; i < n; i++){
                if(distances[i] != 0 && values[i] == null){
                    long key = keys[i];
                    return new Point(unpackX(key), unpackY(key));
                }
            }
        } else{
            for(int i = 0, n = distances.length; i < n; i++){
                if(distances[i] != 0 && e.equals(values[i])){
                    long key = keys[i];
                    return new Point(unpackX(key), unpackY(key));
                }
            }
        }
        return null;
    }

    @Override
    public void setAll(Grid<? extends E> grid){
        if(grid instanceof HashGrid<? extends E> hashGrid){
            int[] distances = hashGrid.distances;
            long[] keys = hashGrid.keys;
            Object[] values = hashGrid.values;

            for(int i = 0, n = distances.length; i < n; i++)
                if(distances[i] != 0)
                    rawPut(keys[i], values[i]);
        } else
            for(Cell<? extends E> cell : grid.cells())
                rawPut(pack(cell.x, cell.y), cell.value);
    }

    @Override
    public void clear(){
        Arrays.fill(distances, 0);
        Arrays.fill(values, null);
        size = 0;
    }

    @Override
    public Iterable<Point> points(){
        return () -> new HashGridIterator<Point>(){
            @Override
            public Point next(){
                long key = keys[nextSlot()];
                return new Point(unpackX(key), unpackY(key));
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new HashGridIterator<Cell<E>>(){
            @Override
            public Cell<E> next(){
                int i = nextSlot();
                long key = keys[i];
                return new Cell<>(unpackX(key), unpackY(key), (E) values[i]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new HashGridIterator<E>(){
            @Override
            public E next(){
                return (E) values[nextSlot()];
            }
        };
    }

    private abstract class HashGridIterator<T> implements Iterator<T>{
        final int start;
        int cursor, lastSlot = -1;

        HashGridIterator(){
            int[] distances = HashGrid.this.distances;
            int i = 0;
            while(distances[i] != 0)
                i++;
            start = i;
        }

        @Override
        public boolean hasNext(){
            int[] distances = HashGrid.this.distances;
            int mask = HashGrid.this.mask;
            while(cursor <= mask){
                if(distances[(start + cursor) & mask] != 0)
                    return true;
                cursor++;
            }
            return false;
        }

        final int nextSlot(){
            if(!hasNext())
                throw new NoSuchElementException();
            return lastSlot = (start + cursor++) & mask;
        }

        @Override
        public void remove(){
            if(lastSlot < 0)
                throw new IllegalStateException();
            shiftDelete(lastSlot);
            cursor = (lastSlot - start) & mask;
            lastSlot = -1;
        }
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(!(obj instanceof HashGrid<?> hashGrid) || size != hashGrid.size)
            return false;
        int[] distances = this.distances;
        long[] keys = this.keys;
        Object[] values = this.values;
        Object[] otherValues = hashGrid.values;

        for(int i = 0, n = distances.length; i < n; i++){
            if(distances[i] != 0){
                int j = hashGrid.findSlot(keys[i]);
                if(j < 0 || !Objects.equals(values[i], otherValues[j]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode(){
        int[] distances = this.distances;
        long[] keys = this.keys;
        Object[] values = this.values;

        int h = 0;
        for(int i = 0, n = distances.length; i < n; i++){
            if(distances[i] != 0){
                long k = keys[i];
                int kh = (int) (k ^ (k >>> 32));
                h += kh ^ Objects.hashCode(values[i]);
            }
        }
        return h;
    }
}
