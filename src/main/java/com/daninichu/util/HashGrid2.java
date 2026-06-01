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
 * avoiding {@link Grid.Point} allocation on lookups. Slot state (empty / live / tombstone)
 * is tracked in a parallel {@code byte[]} so that no key value is ever reserved as a sentinel —
 * every possible {@code (x,y)} coordinate is a valid key.
 * <p>
 * The table size is always a power of two so slot indices can be computed with a bitwise AND.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid2<E> extends AbstractGrid<E> implements Grid<E>{
    private static final byte EMPTY = 0;
    private static final byte LIVE = 1;
    private static final byte TOMBSTONE = 2;

    private long[] keys;
    private Object[] vals;
    private byte[] state;
    private int size, threshold;
    private float loadFactor;

    public HashGrid2(){
        this.loadFactor = 0.75f;
        init(16);
    }

    public HashGrid2(int initialCapacity){
        this(initialCapacity, 0.75f);
    }

    public HashGrid2(int initialCapacity, float loadFactor){
        if(initialCapacity <= 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if(!(0 < loadFactor && loadFactor < 1))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        init(initialCapacity == 1? 1 : Integer.highestOneBit(initialCapacity - 1) << 1);
    }

    public HashGrid2(Grid<? extends E> grid){
        this();
        setAll(grid);
    }

    private void init(int capacity){
        keys = new long[capacity];
        vals = new Object[capacity];
        state = new byte[capacity];
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
     * The insert slot is the first tombstone in the probe chain, or the first empty slot.
     */
    private int findSlot(long key){
        int mask = keys.length - 1;
        int i = hash(key) & mask;
        while(true){
            byte s = state[i];
            if(s == EMPTY)
                return ~i;
            if(s == LIVE && keys[i] == key)
                return i;
            i = (i + 1) & mask;
        }
    }

    private int findSlotForInsert(long key){
        long[] keys = this.keys;
        byte[] state = this.state;

        int mask = state.length - 1;
        int i = hash(key) & mask;
        int tombstone = -1;
        while(true){
            byte s = state[i];
            if(s == EMPTY)
                return tombstone < 0? ~i : ~tombstone;
            if(s == LIVE && keys[i] == key)
                return i;
            if(s == TOMBSTONE && tombstone < 0)
                tombstone = i;
            i = (i + 1) & mask;
        }
    }

    private E rawPut(long key, E value){
        int i = findSlotForInsert(key);
        if(i >= 0){
            E oldValue = (E) vals[i];
            vals[i] = value;
            return oldValue;
        }
        i = ~i;
        if(state[i] != TOMBSTONE && size >= threshold){
            resize();
            i = ~findSlotForInsert(key);
        }
        keys[i] = key;
        vals[i] = value;
        state[i] = LIVE;
        size++;
        return null;
    }

    private E rawRemove(long key){
        long[] keys = this.keys;
        byte[] state = this.state;

        int mask = state.length - 1;
        int i = hash(key) & mask;
        while(true){
            byte s = state[i];
            if(s == EMPTY)
                return null;
            if(s == LIVE && keys[i] == key){
                E oldValue = (E) vals[i];
                state[i] = TOMBSTONE;
                vals[i] = null;
                size--;
                return oldValue;
            }
            i = (i + 1) & mask;
        }
    }

    private void resize(){
        long[] oldKeys = this.keys;
        Object[] oldVals = this.vals;
        byte[] oldState = this.state;

        int oldCap = keys.length;
        int newCap = oldCap << 1;

        long[] keys = this.keys = new long[newCap];
        Object[] vals = this.vals = new Object[newCap];
        byte[] state = this.state = new byte[newCap];
        threshold = (int) (newCap * loadFactor);

        int mask = newCap - 1;
        for(int i = 0; i < oldCap; i++){
            if(oldState[i] == LIVE){
                long k = oldKeys[i];
                int j = hash(k) & mask;
                while(state[j] == LIVE){
                    j = (j + 1) & mask;
                }
                keys[j] = k;
                vals[j] = oldVals[i];
                state[j] = LIVE;
            }
        }
    }

    @Override
    public E get(int x, int y){
        int i = findSlot(pack(x, y));
        return i >= 0? (E) vals[i] : null;
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
        if(grid instanceof HashGrid2<? extends E> other){
            for(int i = 0; i < other.keys.length; i++){
                if(other.state[i] == LIVE)
                    rawPut(other.keys[i], (E) other.vals[i]);
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
        Object[] vals = this.vals;
        byte[] state = this.state;
        if(e == null){
            for(int i = 0, n = state.length; i < n; i++){
                if(state[i] == LIVE && vals[i] == null){
                    state[i] = TOMBSTONE;
                    vals[i] = null;
                    size--;
                    return true;
                }
            }
        } else{
            for(int i = 0, n = state.length; i < n; i++){
                if(state[i] == LIVE && e.equals(vals[i])){
                    state[i] = TOMBSTONE;
                    vals[i] = null;
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
        Object[] vals = this.vals;
        byte[] state = this.state;
        if(e == null){
            for(int i = 0, n = state.length; i < n; i++){
                if(state[i] == LIVE && vals[i] == null)
                    return true;
            }
        } else{
            for(int i = 0, n = state.length; i < n; i++){
                if(state[i] == LIVE && e.equals(vals[i]))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        Object[] vals = this.vals;
        byte[] state = this.state;
        if(e == null){
            for(int i = 0, n = state.length; i < n; i++){
                if(state[i] == LIVE && vals[i] == null){
                    long key = keys[i];
                    return new Point(unpackX(key), unpackY(key));
                }
            }
        } else{
            for(int i = 0, n = state.length; i < n; i++){
                if(state[i] == LIVE && e.equals(vals[i])){
                    long key = keys[i];
                    return new Point(unpackX(key), unpackY(key));
                }
            }
        }
        return null;
    }

    @Override
    public void clear(){
        Arrays.fill(state, EMPTY);
        Arrays.fill(vals, null);
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
                return new Cell<>(unpackX(key), unpackY(key), (E) vals[lastSlot]);
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
                return (E) vals[lastSlot = cursor++];
            }
        };
    }

    private abstract class HashGridIterator<T> implements Iterator<T>{
        int cursor, lastSlot = -1;

        @Override
        public boolean hasNext(){
            while(cursor < state.length){
                if(state[cursor] == LIVE)
                    return true;
                cursor++;
            }
            return false;
        }

        @Override
        public void remove(){
            if(lastSlot < 0)
                throw new IllegalStateException();
            state[lastSlot] = TOMBSTONE;
            vals[lastSlot] = null;
            size--;
            lastSlot = -1;
        }
    }

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator){
        long key = pack(x, y);
        int i = findSlotForInsert(key);
        if(i >= 0){
            vals[i] = operator.apply((E) vals[i]);
        } else{
            i = ~i;
            if(state[i] != TOMBSTONE && size >= threshold){
                resize();
                i = ~findSlotForInsert(key);
            }
            keys[i] = key;
            vals[i] = operator.apply(null);
            state[i] = LIVE;
            size++;
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
        if(!(obj instanceof HashGrid2<?> hashGrid) || size != hashGrid.size){
            return false;
        }
        byte[] state = this.state;
        for(int i = 0, n = state.length; i < n; i++){
            if(state[i] == LIVE){
                int j = hashGrid.findSlot(keys[i]);
                if(j < 0 || !Objects.equals(vals[i], hashGrid.vals[j]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode(){
        long[] keys = this.keys;
        Object[] vals = this.vals;
        byte[] state = this.state;

        int h = 0;
        for(int i = 0, n = state.length; i < n; i++){
            if(state[i] == LIVE){
                long k = keys[i];
                int kh = (int) (k ^ (k >>> 32));
                h += kh ^ Objects.hashCode(vals[i]);
            }
        }
        return h;
    }
}