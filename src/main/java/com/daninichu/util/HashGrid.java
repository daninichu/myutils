package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A hash-based Grid backed by a custom open-addressing hash table with linear probing.
 * <p>
 * Keys are stored as packed {@code long}s (x in the high 32 bits, y in the low 32 bits),
 * avoiding {@link Point} allocation on lookups.
 * <p>
 * The table size is always a power of two so slot indices can be computed with a bitwise AND.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid<E> extends AbstractGrid<E> implements Grid<E>{
    private boolean[] occupied;
    private long[] keys;
    private Object[] values;

    private int size, threshold;
    private final float loadFactor;

    public HashGrid(){
        this.loadFactor = 0.75f;
        init(16);
    }

    public HashGrid(int initialCapacity){
        this(initialCapacity, 0.75f);
    }

    public HashGrid(int initialCapacity, float loadFactor){
        if(initialCapacity < 2)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if(!(0 < loadFactor && loadFactor < 1))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        init(Integer.highestOneBit(initialCapacity - 1) << 1);
    }

    public HashGrid(Grid<? extends E> grid){
        this();
        setAll(grid);
    }

    private void init(int capacity){
        occupied = new boolean[capacity];
        keys = new long[capacity];
        values = new Object[capacity];
        threshold = (int) (capacity * loadFactor);
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
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return (int) (key ^ (key >>> 32));
    }

    /**
     * Returns the slot index of {@code key} if present,
     * or {@code ~insertSlot} (a negative value) if absent.
     * The insert slot is the first empty slot.
     */
    private int findSlot(long key){
        boolean[] occupied = this.occupied;
        long[] keys = this.keys;

        int mask = occupied.length - 1;
        int i = hash(key) & mask;
        while(true){
            if(!occupied[i])
                return ~i;
            if(keys[i] == key)
                return i;
            i = (i + 1) & mask;
        }
    }

    private E rawPut(long key, E value){
        int i = findSlot(key);
        if(i >= 0){
            E oldValue = (E) values[i];
            values[i] = value;
            return oldValue;
        }
        i = ~i;
        if(size >= threshold){
            resize();
            i = ~findSlot(key);
        }
        occupied[i] = true;
        keys[i] = key;
        values[i] = value;
        size++;
        return null;
    }

    private E rawRemove(long key){
        boolean[] occupied = this.occupied;
        long[] keys = this.keys;

        int mask = occupied.length - 1;
        int i = hash(key) & mask;
        while(true){
            if(!occupied[i])
                return null;
            if(keys[i] == key){
                E oldValue = (E) values[i];
                shiftDelete(i);
                size--;
                return oldValue;
            }
            i = (i + 1) & mask;
        }
    }

    private void shiftDelete(int hole){
        boolean[] occupied = this.occupied;
        long[] keys = this.keys;
        Object[] values = this.values;

        int mask = occupied.length - 1;
        int next = (hole + 1) & mask;
        while(occupied[next]){
            int home = hash(keys[next]) & mask;
            if(((next - home) & mask) > ((hole - home) & mask)){
                keys[hole] = keys[next];
                values[hole] = values[next];
                hole = next;
            }
            next = (next + 1) & mask;
        }
        occupied[hole] = false;
        values[hole] = null;
    }

    private void resize(){
        boolean[] oldOccupied = this.occupied;
        long[] oldKeys = this.keys;
        Object[] oldValues = this.values;

        int oldCap = keys.length;
        int newCap = oldCap << 1;

        boolean[] occupied = this.occupied = new boolean[newCap];
        long[] keys = this.keys = new long[newCap];
        Object[] values = this.values = new Object[newCap];
        threshold = (int) (newCap * loadFactor);

        int mask = newCap - 1;
        for(int i = 0; i < oldCap; i++){
            if(oldOccupied[i]){
                long k = oldKeys[i];
                int j = hash(k) & mask;
                while(occupied[j]){
                    j = (j + 1) & mask;
                }
                occupied[j] = true;
                keys[j] = k;
                values[j] = oldValues[i];
            }
        }
    }

    @Override
    public E get(int x, int y){
        int i = findSlot(pack(x, y));
        return i >= 0? (E) values[i] : null;
    }

    @Override
    public E get(Point p){
        return get(p.x, p.y);
    }

    @Override
    public E set(int x, int y, E e){
        return rawPut(pack(x, y), e);
    }

    @Override
    public E set(Point p, E e){
        return rawPut(pack(p.x, p.y), e);
    }

    @Override
    public void setAll(Grid<? extends E> grid){
        if(grid instanceof HashGrid<? extends E> other){
            long[] keys = other.keys;
            Object[] values = other.values;
            boolean[] occupied = other.occupied;

            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i])
                    rawPut(keys[i], (E) values[i]);
            }
        } else{
            for(Cell<? extends E> cell : grid.cells())
                rawPut(pack(cell.x, cell.y), cell.value);
        }
    }

    @Override
    public E removePoint(int x, int y){
        return rawRemove(pack(x, y));
    }

    @Override
    public E removePoint(Point p){
        return rawRemove(pack(p.x, p.y));
    }

    @Override
    public boolean removeValue(E e){
        boolean[] occupied = this.occupied;
        Object[] values = this.values;

        if(e == null){
            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i] && values[i] == null){
                    shiftDelete(i);
                    size--;
                    return true;
                }
            }
        } else{
            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i] && e.equals(values[i])){
                    shiftDelete(i);
                    size--;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsPoint(int x, int y){
        return findSlot(pack(x, y)) >= 0;
    }

    @Override
    public boolean containsPoint(Point p){
        return findSlot(pack(p.x, p.y)) >= 0;
    }

    @Override
    public boolean containsValue(E e){
        boolean[] occupied = this.occupied;
        Object[] values = this.values;

        if(e == null){
            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i] && values[i] == null)
                    return true;
            }
        } else{
            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i] && e.equals(values[i]))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        boolean[] occupied = this.occupied;
        Object[] values = this.values;

        if(e == null){
            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i] && values[i] == null){
                    long key = keys[i];
                    return new Point(unpackX(key), unpackY(key));
                }
            }
        } else{
            for(int i = 0, n = occupied.length; i < n; i++){
                if(occupied[i] && e.equals(values[i])){
                    long key = keys[i];
                    return new Point(unpackX(key), unpackY(key));
                }
            }
        }
        return null;
    }

    @Override
    public void clear(){
        Arrays.fill(occupied, false);
        Arrays.fill(values, null);
        size = 0;
    }

    @Override
    public Iterable<Point> points(){
        return () -> new HashGridIterator<Point>(){
            @Override
            public Point next(){
                if(!hasNext())
                    throw new NoSuchElementException();
                long key = keys[lastSlot = cursor++];
                return new Point(unpackX(key), unpackY(key));
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new HashGridIterator<Cell<E>>(){
            @Override
            public Cell<E> next(){
                if(!hasNext())
                    throw new NoSuchElementException();
                long key = keys[lastSlot = cursor++];
                return new Cell<>(unpackX(key), unpackY(key), (E) values[lastSlot]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new HashGridIterator<E>(){
            @Override
            public E next(){
                if(!hasNext())
                    throw new NoSuchElementException();
                return (E) values[lastSlot = cursor++];
            }
        };
    }

    private abstract class HashGridIterator<T> implements Iterator<T>{
        int cursor, lastSlot = -1;

        @Override
        public boolean hasNext(){
            boolean[] occupied = HashGrid.this.occupied;
            int n = occupied.length;
            while(cursor < n){
                if(occupied[cursor])
                    return true;
                cursor++;
            }
            return false;
        }

        @Override
        public void remove(){
            if(lastSlot < 0)
                throw new IllegalStateException();
            shiftDelete(cursor = lastSlot);
            size--;
            lastSlot = -1;
        }
    }

    public int size(){
        return size;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(!(obj instanceof HashGrid<?> hashGrid) || size != hashGrid.size){
            return false;
        }
        boolean[] occupied = this.occupied;
        long[] keys = this.keys;
        Object[] values = this.values;
        Object[] otherValues = hashGrid.values;

        for(int i = 0, n = occupied.length; i < n; i++){
            if(occupied[i]){
                int j = hashGrid.findSlot(keys[i]);
                if(j < 0 || !Objects.equals(values[i], otherValues[j]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode(){
        boolean[] occupied = this.occupied;
        long[] keys = this.keys;
        Object[] values = this.values;

        int h = 0;
        for(int i = 0, n = occupied.length; i < n; i++){
            if(occupied[i]){
                long k = keys[i];
                int kh = (int) (k ^ (k >>> 32));
                h += kh ^ Objects.hashCode(values[i]);
            }
        }
        return h;
    }
}