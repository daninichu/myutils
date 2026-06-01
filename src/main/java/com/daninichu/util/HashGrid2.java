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
 * every possible {@code (x, y)} coordinate is a valid key.
 * <p>
 * The table size is always a power of two so slot indices can be computed with a bitwise AND.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid2<E> extends AbstractGrid<E> implements Grid<E>{

    private static final byte EMPTY     = 0;
    private static final byte LIVE      = 1;
    private static final byte TOMBSTONE = 2;

    private static final int   DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR      = 0.75f;

    private long[]   keys;
    private Object[] vals;
    private byte[]   state;
    private int      size;
    private int      threshold;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

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
        keys      = new long[capacity];
        vals      = new Object[capacity];
        state     = new byte[capacity];     // all EMPTY (0) by default
        threshold = (int)(capacity * LOAD_FACTOR);
    }

    // -------------------------------------------------------------------------
    // Key packing / unpacking
    // -------------------------------------------------------------------------

    private static long pack(int x, int y){
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static int unpackX(long key){ return (int)(key >>> 32); }
    private static int unpackY(long key){ return (int) key; }

    // -------------------------------------------------------------------------
    // Hashing
    // -------------------------------------------------------------------------

    private static int hash(long key){
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return (int)(key ^ (key >>> 32));
//        return 65537 * unpackX(key) + unpackY(key);
    }

    // -------------------------------------------------------------------------
    // Internal probing
    // -------------------------------------------------------------------------

    /**
     * Returns the slot index of {@code key} if present,
     * or {@code ~insertSlot} (a negative value) if absent.
     * The insert slot is the first tombstone in the probe chain, or the first empty slot.
     */
    private int findSlot(long key){
        int mask = keys.length - 1;
        int i    = hash(key) & mask;
        while(true){
            byte s = state[i];
            if(s == EMPTY)                   return ~i;
            if(s == LIVE && keys[i] == key)  return i;
            i = (i + 1) & mask;
        }
    }

    private int findSlotForInsert(long key){
        int mask      = keys.length - 1;
        int i         = hash(key) & mask;
        int tombstone = -1;
        while(true){
            byte s = state[i];
            if(s == EMPTY)                  return tombstone < 0 ? ~i : ~tombstone;
            if(s == LIVE && keys[i] == key) return i;
            if(s == TOMBSTONE && tombstone < 0) tombstone = i;
            i = (i + 1) & mask;
        }
    }

    // -------------------------------------------------------------------------
    // Internal get / put / remove
    // -------------------------------------------------------------------------

    private E rawGet(long key){
        int i = findSlot(key);
        return i >= 0 ? (E) vals[i] : null;
    }

    private E rawPut(long key, E value){
        int i = findSlotForInsert(key);
        if(i >= 0){
            E old = (E) vals[i];
            vals[i] = value;
            return old;
        }
        i = ~i;
        boolean reusedTombstone = state[i] == TOMBSTONE;
        if(!reusedTombstone && size >= threshold){
            resize();
            i = ~findSlotForInsert(key);
        }
        keys[i]  = key;
        vals[i]  = value;
        state[i] = LIVE;
        size++;
        return null;
    }

    private E rawRemove(long key){
        int mask = keys.length - 1;
        int i    = hash(key) & mask;
        while(true){
            byte s = state[i];
            if(s == EMPTY) return null;
            if(s == LIVE && keys[i] == key){
                E old    = (E) vals[i];
                state[i] = TOMBSTONE;
                vals[i]  = null;
                size--;
                return old;
            }
            i = (i + 1) & mask;
        }
    }

    // -------------------------------------------------------------------------
    // Resize
    // -------------------------------------------------------------------------

    private void resize(){
        int      newCap   = keys.length << 1;
        long[]   newKeys  = new long[newCap];
        Object[] newVals  = new Object[newCap];
        byte[]   newState = new byte[newCap];
        int      mask     = newCap - 1;
        for(int i = 0; i < keys.length; i++){
            if(state[i] != LIVE) continue;
            long k = keys[i];
            int j = hash(k) & mask;
            while(newState[j] == LIVE) j = (j + 1) & mask;
            newKeys[j]  = k;
            newVals[j]  = vals[i];
            newState[j] = LIVE;
        }
        keys      = newKeys;
        vals      = newVals;
        state     = newState;
        threshold = (int)(newCap * LOAD_FACTOR);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static int tableSizeFor(int n){
        if(n <= 1) return DEFAULT_CAPACITY;
        n = Integer.highestOneBit(n - 1) << 1;
        return Math.max(n, DEFAULT_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // Grid implementation
    // -------------------------------------------------------------------------

    @Override
    public E get(int x, int y){
        return rawGet(pack(x, y));
    }

    @Override
    public E get(Point p){
        Objects.requireNonNull(p);
        return rawGet(pack(p.x, p.y));
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
        Objects.requireNonNull(p);
        return rawRemove(pack(p.x, p.y));
    }

    @Override
    public boolean removeValue(E e){
        for(int i = 0; i < state.length; i++){
            if(state[i] == LIVE && Objects.equals(e, vals[i])){
                state[i] = TOMBSTONE;
                vals[i]  = null;
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
            if(state[i] == LIVE && Objects.equals(e, vals[i]))
                return true;
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        for(int i = 0; i < state.length; i++){
            if(state[i] == LIVE && Objects.equals(e, vals[i]))
                return new Point(unpackX(keys[i]), unpackY(keys[i]));
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
        return () -> new HashGridIterator<>(){
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
                return new Cell<>(unpackX(keys[slot]), unpackY(keys[slot]), (E) vals[slot]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new HashGridIterator<>(){
            @Override
            protected E produce(int slot){
                return (E) vals[slot];
            }
        };
    }

    private abstract class HashGridIterator<T> implements Iterator<T>{
        private int lastSlot = -1;
        private int cursor   = 0;

        @Override
        public boolean hasNext(){
            while(cursor < state.length){
                if(state[cursor] == LIVE) return true;
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
            if(lastSlot < 0) throw new IllegalStateException();
            state[lastSlot] = TOMBSTONE;
            vals[lastSlot]  = null;
            size--;
            lastSlot = -1;
        }

        protected abstract T produce(int slot);
    }

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator){
        long key = pack(x, y);
        int i = findSlotForInsert(key);
        if(i >= 0){
            vals[i] = operator.apply((E) vals[i]);
        } else{
            i = ~i;
            boolean reusedTombstone = state[i] == TOMBSTONE;
            if(!reusedTombstone && size >= threshold){
                resize();
                i = ~findSlotForInsert(key);
            }
            keys[i]  = key;
            vals[i]  = operator.apply(null);
            state[i] = LIVE;
            size++;
        }
    }

    // -------------------------------------------------------------------------
    // Size / conversion
    // -------------------------------------------------------------------------

    public int size(){ return size; }

    public java.util.HashMap<Point, E> toMap(){
        var map = new java.util.HashMap<Point, E>(size * 2);
        for(int i = 0; i < state.length; i++){
            if(state[i] == LIVE)
                map.put(new Point(unpackX(keys[i]), unpackY(keys[i])), (E) vals[i]);
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(!(obj instanceof HashGrid2<?> other)) return false;
        if(size != other.size) return false;
        for(int i = 0; i < state.length; i++){
            if(state[i] != LIVE) continue;
            int j = other.findSlot(keys[i]);
            if(j < 0) return false;
            if(!Objects.equals(vals[i], other.vals[j])) return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        int h = 0;
        for(int i = 0; i < state.length; i++){
            if(state[i] != LIVE) continue;
            long k = keys[i];
            int kh = (int)(k ^ (k >>> 32));
            h += kh ^ Objects.hashCode(vals[i]);
        }
        return h;
    }
}