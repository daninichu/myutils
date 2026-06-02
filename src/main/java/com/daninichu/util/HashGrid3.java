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
public class HashGrid3<E> extends AbstractGrid<E> implements Grid<E>{
    private static final byte EMPTY = 0;
    private static final byte LIVE = 1;

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.9f;

    private long[] keys;
    private Object[] values;
    private byte[] state;
    private int size;
    private int threshold;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public HashGrid3(){
        init(DEFAULT_CAPACITY);
    }

    public HashGrid3(int initialCapacity){
        init(tableSizeFor(initialCapacity));
    }

    public HashGrid3(Grid<? extends E> grid){
        init(DEFAULT_CAPACITY);
        setAll(grid);
    }

    private void init(int capacity){
        keys = new long[capacity];
        values = new Object[capacity];
        state = new byte[capacity];
        threshold = (int)(capacity * LOAD_FACTOR);
    }

    // -------------------------------------------------------------------------
    // Key packing / unpacking
    // -------------------------------------------------------------------------

    private static long pack(int x, int y){
        return (long) x << 32 | y & 0xFFFFFFFFL;
    }

    private static int unpackX(long key){
        return (int) (key >>> 32);
    }

    private static int unpackY(long key){
        return (int) key;
    }

    // -------------------------------------------------------------------------
    // Hashing
    // -------------------------------------------------------------------------

    private static int hash(long key){
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return (int) (key ^ (key >>> 32));
    }

    private int idealSlot(long key){
        return hash(key) & (keys.length - 1);
    }

    /**
     * Distance from initial bucket: how many slots the key at {@code slot}
     * has travelled from its ideal position.
     */
    private int dib(int slot){
        return (slot - idealSlot(keys[slot]) + keys.length) & (keys.length - 1);
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

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
            int dib = (i - (hash(other) & mask) + len) & mask;
            if(dib < probe)
                return -1;
            i = (i + 1) & mask;
            probe++;
        }
    }

    // -------------------------------------------------------------------------
    // Insertion (Robin Hood)
    // -------------------------------------------------------------------------

    /**
     * Inserts or updates {@code key -> value}.
     * Returns the previous value, or null if absent.
     */
    private E rawPut(long key, E value){
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
                size++;
                return null;
            }

            if(keys[i] == insertKey){
                // Key already present — update value only, no size change.
                // If we swapped along the way, we need to restore: but we only
                // swap when dib(i) < probe, meaning the slot's key != insertKey
                // (we'd have matched earlier). So this branch is only reached
                // before any swap — safe to return old value directly.
                E oldValue = (E) values[i];
                values[i] = insertVal;
                return oldValue;
            }

            // Robin Hood: if the resident is "richer" (closer to home) than us, evict it.
            int residentDib = dib(i);
            if(residentDib < probe){
                // Swap: we take this slot, and continue inserting the evicted entry.
                long tmpKey = keys[i];
                Object tmpVal = values[i];
                keys[i] = insertKey;
                values[i] = insertVal;
                insertKey = tmpKey;
                insertVal = tmpVal;
                probe = residentDib;
            }

            i = (i + 1) & mask;
            probe++;
        }
    }

    // -------------------------------------------------------------------------
    // Deletion (backward shift — no tombstones needed)
    // -------------------------------------------------------------------------

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
            if(state[next] == EMPTY || dib(next) == 0){
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

    // -------------------------------------------------------------------------
    // Resize
    // -------------------------------------------------------------------------

    private void resize(){
        int newCap = keys.length << 1;
        long[] oldKeys = keys;
        Object[] oldVals = values;
        byte[] oldState = state;
        init(newCap);
        size = 0;
        for(int i = 0; i < oldKeys.length; i++){
            if(oldState[i] == LIVE)
                rawPut(oldKeys[i], (E) oldVals[i]);
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
        if(grid instanceof HashGrid3<? extends E> other){
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
            protected Point produce(int slot){
                return new Point(unpackX(keys[slot]), unpackY(keys[slot]));
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new HashGridIterator<Cell<E>>(){
            @Override
            protected Cell<E> produce(int slot){
                return new Cell<>(unpackX(keys[slot]), unpackY(keys[slot]), (E) values[slot]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new HashGridIterator<E>(){
            @Override
            protected E produce(int slot){
                return (E) values[slot];
            }
        };
    }

    private abstract class HashGridIterator<T> implements Iterator<T>{
        private int cursor, lastSlot = -1;

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
        public T next(){
            while(cursor < state.length){
                if(state[cursor] == LIVE){
                    lastSlot = cursor++;
                    return produce(lastSlot);
                }
                cursor++;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove(){
            if(lastSlot < 0)
                throw new IllegalStateException();
            backwardShift(lastSlot);
            size--;
            // Backward shift may have pulled a later entry into lastSlot.
            // Step cursor back so hasNext()/next() re-examines it.
            cursor = lastSlot;
            lastSlot = -1;
        }

        protected abstract T produce(int slot);
    }

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator){
        long key  = pack(x, y);
        int  slot = findSlot(key);
        if(slot >= 0){
            values[slot] = operator.apply((E) values[slot]);
        } else{
            rawPut(key, operator.apply(null));
        }
    }

    // -------------------------------------------------------------------------
    // Size / conversion
    // -------------------------------------------------------------------------

    public int size(){
        return size;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(!(obj instanceof HashGrid3<?> other))
            return false;
        if(size != other.size)
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
