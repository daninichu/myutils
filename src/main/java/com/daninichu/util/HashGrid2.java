package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A hash-based Grid backed by a custom open-addressing hash table with Robin Hood linear probing.
 * <p>
 * Keys are stored as packed {@code long}s (x in the high 32 bits, y in the low 32 bits).
 * Slot state is tracked in a parallel {@code byte[]} so no key value is reserved as a sentinel.
 * <p>
 * Robin Hood insertion: when inserting a key K at slot i, if the resident key R has a shorter
 * probe distance (DIB) than K, K evicts R and continues inserting R. This equalises probe
 * lengths across all entries, bounding worst-case lookup cost.
 * <p>
 * As a result of the reduced variance, tombstone-free deletion via backward shift is viable.
 * Deleted slots are immediately reclaimed rather than marked, so the table never degrades
 * under mixed insert/delete workloads.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid2<E> extends AbstractGrid<E> implements Grid<E>{
    private static final byte EMPTY = 0;
    private static final byte LIVE = 1;

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.9f;

    private byte[] state;
    private int[] dib;
    private long[] keys;
    private Object[] values;

    private int size;
    private int threshold;

    public HashGrid2(){
        init(DEFAULT_CAPACITY);
    }

    public HashGrid2(int initialCapacity){
        init(tableSizeFor(initialCapacity));
    }

    public HashGrid2(Grid<? extends E> grid){
        init(DEFAULT_CAPACITY);
        setAll(grid);
    }

    private void init(int capacity){
        keys = new long[capacity];
        values = new Object[capacity];
        state = new byte[capacity];
        dib = new int[capacity];
        threshold = (int)(capacity * LOAD_FACTOR);
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
     * Distance from initial bucket: how many slots the key at {@code slot}
     * has travelled from its ideal position.
     */
    private int dib(int slot){
        int n = keys.length;
        return (slot - (hash(keys[slot]) & (n - 1)) + n) & (n - 1);
    }

    /**
     * Returns the slot index of {@code key}, or -1 if absent.
     * <p>
     * Robin Hood invariant: every key in a probe chain has DIB >= the previous one.
     * So if we ever encounter a key whose DIB is less than our own probe distance,
     * the target key cannot be further in the chain — stop early.
     */
    private int findSlot(long key){
        long[] keys = this.keys;
        byte[] state = this.state;

        int len = keys.length;
        int mask = len - 1;
        int i = hash(key) & mask;
        int probe = 0;
        while(true){
            if(state[i] == EMPTY)
                return -1;
            long other = keys[i];
            if(other == key)
                return i;
            if(dib[i] < probe)
                return -1;
            i = (i + 1) & mask;
            probe++;
        }
    }

    /**
     * Inserts or updates {@code key -> value}.
     * Returns the previous value, or null if absent.
     */
    private E rawPut(long key, Object value){
        if(size >= threshold){
            resize();
        }
        int mask = keys.length - 1;
        int i = hash(key) & mask;
        int probe = 0;

        long insertKey = key;
        Object insertVal = value;

        while(true){
            if(state[i] == EMPTY){
                keys[i] = insertKey;
                values[i] = insertVal;
                state[i] = LIVE;
                dib[i] = probe;
                size++;
                return null;
            }

            if(keys[i] == insertKey){
                E oldValue = (E) values[i];
                values[i] = insertVal;
                return oldValue;
            }

            // Robin Hood: if the resident is "richer" (closer to home) than us, evict it.
            int residentDib = dib[i];
            if(residentDib < probe){
                // Swap: we take this slot, and continue inserting the evicted entry.
                long tmpKey = keys[i];
                Object tmpVal = values[i];
                keys[i] = insertKey;
                values[i] = insertVal;
                dib[i] = probe;
                insertKey = tmpKey;
                insertVal = tmpVal;
                probe = residentDib;
            }

            i = (i + 1) & mask;
            probe++;
        }
    }

    /**
     * Removes {@code key} and returns its value, or null if absent.
     * <p>
     * After removing a slot, entries immediately following it are shifted
     * backward as long as they would benefit (i.e. their DIB > 0).
     * This keeps the Robin Hood invariant intact without tombstones.
     */
    private E rawRemove(long key){
        int slot = findSlot(key);
        if(slot < 0){
            return null;
        }
        E old = (E) values[slot];
        backwardShift(slot);
        size--;
        return old;
    }

    private void backwardShift(int slot){
        int mask = keys.length - 1;
        int cur = slot;
        while(true){
            int next = (cur + 1) & mask;
            if(state[next] == EMPTY || dib[next] == 0){
                // Next slot is empty or is already at its ideal position — stop.
                state[cur] = EMPTY;
                values[cur] = null;
                return;
            }
            // Pull next slot back by one.
            keys[cur] = keys[next];
            values[cur] = values[next];
            state[cur] = LIVE;
            cur = next;
        }
    }

    private void resize(){
        byte[] oldState = state;
        long[] oldKeys = keys;
        Object[] oldVals = values;

        int oldCap = oldState.length;
        init(oldCap << 1);
        for(int i = size = 0; i < oldCap; i++){
            if(oldState[i] == LIVE)
                rawPut(oldKeys[i], oldVals[i]);
        }
    }

    private static int tableSizeFor(int n){
        if(n <= 1)
            return DEFAULT_CAPACITY;
        n = Integer.highestOneBit(n - 1) << 1;
        return Math.max(n, DEFAULT_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // Grid implementation
    // -------------------------------------------------------------------------

    @Override
    public E get(int x, int y){
        int slot = findSlot(pack(x, y));
        return slot >= 0? (E) values[slot] : null;
    }

    @Override
    public E get(Point p){
        Objects.requireNonNull(p);
        return get(p.x, p.y);
    }

    @Override
    public E set(int x, int y, E e){
        return rawPut(pack(x, y), e);
    }

    @Override
    public E set(Point p, E e){
        Objects.requireNonNull(p);
        return rawPut(pack(p.x, p.y), e);
    }

    @Override
    public void setAll(Grid<? extends E> grid){
        if(grid instanceof HashGrid2<? extends E> other){
            for(int i = 0; i < other.keys.length; i++){
                if(other.state[i] == LIVE)
                    rawPut(other.keys[i], (E) other.values[i]);
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
        Objects.requireNonNull(p);
        return rawRemove(pack(p.x, p.y));
    }

    @Override
    public boolean removeValue(E e){
        for(int i = 0; i < state.length; i++){
            if(state[i] == LIVE && Objects.equals(e, values[i])){
                backwardShift(i);
                size--;
                return true;
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
        Objects.requireNonNull(p);
        return findSlot(pack(p.x, p.y)) >= 0;
    }

    @Override
    public boolean containsValue(E e){
        for(int i = 0; i < state.length; i++){
            if(state[i] == LIVE && Objects.equals(e, values[i]))
                return true;
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        for(int i = 0; i < state.length; i++){
            if(state[i] == LIVE && Objects.equals(e, values[i]))
                return new Point(unpackX(keys[i]), unpackY(keys[i]));
        }
        return null;
    }

    @Override
    public void clear(){
        Arrays.fill(state, EMPTY);
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
            byte[] occupied = HashGrid2.this.state;
            int n = occupied.length;
            while(cursor < n){
                if(occupied[cursor] == LIVE)
                    return true;
                cursor++;
            }
            return false;
        }

        @Override
        public void remove(){
            if(lastSlot < 0)
                throw new IllegalStateException();
            backwardShift(cursor = lastSlot);
            size--;
            lastSlot = -1;
        }
    }

    public int size(){
        return size;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(!(obj instanceof HashGrid2<?> other) || size != other.size)
            return false;
        for(int i = 0; i < state.length; i++){
            if(state[i] != LIVE)
                continue;
            int j = other.findSlot(keys[i]);
            if(j < 0)
                return false;
            if(!Objects.equals(values[i], other.values[j]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        int h = 0;
        for(int i = 0; i < state.length; i++){
            if(state[i] != LIVE)
                continue;
            long k = keys[i];
            h += (int)(k ^ (k >>> 32)) ^ Objects.hashCode(values[i]);
        }
        return h;
    }
}
