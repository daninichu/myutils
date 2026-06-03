package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A hash-based Grid backed by a Hopscotch hash table.
 * <p>
 * <h3>Neighbourhood invariant</h3>
 * Every key K that hashes to slot {@code i} (its "home") is guaranteed to reside in one of the
 * {@value #H} slots {@code [i, i+H)} (mod capacity). Each slot stores an {@code int} bitmap
 * whose bit {@code j} is set when slot {@code (i+j) & mask} holds a key whose home is {@code i}.
 * This bounds every lookup to at most {@value #H} key comparisons regardless of load factor.
 *
 * <h3>Insertion</h3>
 * <ol>
 *   <li>Scan linearly from the home slot for any empty slot.</li>
 *   <li>If the empty slot is within the neighbourhood, place the key there and set the bitmap bit.</li>
 *   <li>If the empty slot is too far, hop it backward: find a slot {@code s} whose neighbourhood
 *       overlaps the empty slot, pick any of its members, move it to the empty slot, and update
 *       both bitmaps. Repeat until the empty slot is within the home neighbourhood.</li>
 *   <li>If no hop is possible, resize and retry.</li>
 * </ol>
 *
 * <h3>Deletion</h3>
 * Clear the key/value, mark the slot empty, and clear the corresponding bit in the home bitmap.
 * No tombstones, no shifting.
 *
 * <h3>Lookup</h3>
 * Read the bitmap at the home slot and iterate over its set bits — typically 1–3 iterations.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid4<E> extends AbstractGrid<E> implements Grid<E>{

    // -------------------------------------------------------------------------
    // Neighbourhood size
    // -------------------------------------------------------------------------

    /**
     * Neighbourhood width. Every key lives within H slots of its home.
     * Must be ≤ 32 (bitmap is an int). 32 gives a good balance between
     * lookup bound and hop flexibility under high load.
     */
    private static final int H = 32;

    // -------------------------------------------------------------------------
    // Slot state
    // -------------------------------------------------------------------------

    private static final byte EMPTY = 0;
    private static final byte LIVE  = 1;

    // -------------------------------------------------------------------------
    // Sizing
    // -------------------------------------------------------------------------

    private static final int   DEFAULT_CAPACITY = 64;   // must be ≥ H and a power of two
    private static final float LOAD_FACTOR      = 0.9f;

    // -------------------------------------------------------------------------
    // Table arrays
    // -------------------------------------------------------------------------

    private long[]   keys;
    private Object[] vals;
    private byte[]   state;
    /**
     * bitmap[i] — bit j is set iff slot (i+j)&mask holds a key whose home is i.
     * Only the low H bits are used.
     */
    private int[]    bitmap;
    private int      mask;       // capacity - 1
    private int      size;
    private int      threshold;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public HashGrid4(){
        init(DEFAULT_CAPACITY);
    }

    public HashGrid4(int initialCapacity){
        init(tableSizeFor(initialCapacity));
    }

    public HashGrid4(Grid<? extends E> grid){
        init(DEFAULT_CAPACITY);
        setAll(grid);
    }

    private void init(int capacity){
        keys      = new long[capacity];
        vals      = new Object[capacity];
        state     = new byte[capacity];
        bitmap    = new int[capacity];
        mask      = capacity - 1;
        threshold = (int)(capacity * LOAD_FACTOR);
        size      = 0;
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

    private static long mix(long key){
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return key;
    }

    private int h1(long h){ return (int)(h >>> 1) & mask; }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    private int findSlot(long key){
        int home = h1(mix(key));
        int map  = bitmap[home];
        while(map != 0){
            int offset = Integer.numberOfTrailingZeros(map);
            int slot   = (home + offset) & mask;
            if(state[slot] == LIVE && keys[slot] == key) return slot;
            map &= map - 1;
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Insertion
    // -------------------------------------------------------------------------

    private E rawPut(long key, E value){
        // Check for existing entry first.
        int home = h1(mix(key));
        int map  = bitmap[home];
        int tmp  = map;
        while(tmp != 0){
            int offset = Integer.numberOfTrailingZeros(tmp);
            int slot   = (home + offset) & mask;
            if(state[slot] == LIVE && keys[slot] == key){
                E old    = (E) vals[slot];
                vals[slot] = value;
                return old;
            }
            tmp &= tmp - 1;
        }

        if(size >= threshold){
            resize();
            return rawPut(key, value);
        }

        // Find a free slot by linear scan from home.
        int free = findFreeSlot(home);
        if(free < 0){
            resize();
            return rawPut(key, value);
        }

        // Hop the free slot into the neighbourhood if necessary.
        free = hopToNeighbourhood(home, free);
        if(free < 0){
            resize();
            return rawPut(key, value);
        }

        // Place the entry.
        int offset      = (free - home) & mask;
        keys[free]      = key;
        vals[free]      = value;
        state[free]     = LIVE;
        bitmap[home]   |= 1 << offset;
        size++;
        return null;
    }

    /**
     * Scans linearly from {@code start} for an empty slot.
     * Returns -1 if none found within the whole table.
     */
    private int findFreeSlot(int start){
        int cap = mask + 1;
        for(int i = 0; i < cap; i++){
            int slot = (start + i) & mask;
            if(state[slot] == EMPTY) return slot;
        }
        return -1;
    }

    /**
     * Hops the empty slot at {@code free} backward toward {@code home} until it falls
     * within {@code [home, home+H)}.
     * <p>
     * At each step, looks for a slot {@code s} in {@code [free-H+1, free)} such that
     * {@code free} is within the neighbourhood of {@code s}'s home, and moves one of
     * {@code s}'s members into {@code free}, freeing up {@code s}.
     *
     * @return the new position of the free slot (within the neighbourhood), or -1 if stuck.
     */
    private int hopToNeighbourhood(int home, int free){
        while(((free - home) & mask) >= H){
            boolean hopped = false;
            // Try each candidate slot that could reach `free` within one hop.
            for(int dist = H - 1; dist >= 1; dist--){
                int s   = (free - dist) & mask;
                int sHome = homeOf(s);
                if(sHome < 0) continue;                             // slot is empty, no home
                int freeOffset = (free - sHome) & mask;
                if(freeOffset >= H) continue;                       // free not in s's neighbourhood
                // Find any member of sHome's neighbourhood that is before `free`.
                int sMap = bitmap[sHome];
                int hop  = -1;
                int tmpMap = sMap;
                while(tmpMap != 0){
                    int o = Integer.numberOfTrailingZeros(tmpMap);
                    int candidate = (sHome + o) & mask;
                    if(candidate != free){
                        hop = candidate;
                        break;
                    }
                    tmpMap &= tmpMap - 1;
                }
                if(hop < 0) continue;

                // Move hop → free.
                int hopOffset  = (hop  - sHome) & mask;
                int freeOff2   = (free - sHome) & mask;
                keys[free]     = keys[hop];
                vals[free]     = vals[hop];
                state[free]    = LIVE;
                state[hop]     = EMPTY;
                vals[hop]      = null;
                bitmap[sHome] &= ~(1 << hopOffset);
                bitmap[sHome] |=   1 << freeOff2;
                free   = hop;
                hopped = true;
                break;
            }
            if(!hopped) return -1;
        }
        return free;
    }

    /**
     * Returns the home slot of the key currently at {@code slot}, or -1 if empty.
     */
    private int homeOf(int slot){
        if(state[slot] == EMPTY) return -1;
        return h1(mix(keys[slot]));
    }

    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    private E rawRemove(long key){
        int slot = findSlot(key);
        if(slot < 0) return null;

        E old      = (E) vals[slot];
        int home   = h1(mix(key));
        int offset = (slot - home) & mask;
        bitmap[home] &= ~(1 << offset);
        state[slot]   = EMPTY;
        vals[slot]    = null;
        size--;
        return old;
    }

    // -------------------------------------------------------------------------
    // Resize
    // -------------------------------------------------------------------------

    private void resize(){
        long[]   oldKeys   = keys;
        Object[] oldVals   = vals;
        byte[]   oldState  = state;
        int      oldCap    = mask + 1;
        init(oldCap << 1);
        for(int i = 0; i < oldCap; i++){
            if(oldState[i] == LIVE)
                rawPut(oldKeys[i], (E) oldVals[i]);
        }
    }

    private static int tableSizeFor(int n){
        if(n <= DEFAULT_CAPACITY) return DEFAULT_CAPACITY;
        int p = Integer.highestOneBit(n - 1) << 1;
        return Math.max(p, DEFAULT_CAPACITY);
    }

    // -------------------------------------------------------------------------
    // Grid implementation
    // -------------------------------------------------------------------------

    @Override
    public E get(int x, int y){
        int slot = findSlot(pack(x, y));
        return slot >= 0 ? (E) vals[slot] : null;
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
        if(grid instanceof HashGrid4<? extends E> other){
            int otherCap = other.mask + 1;
            for(int i = 0; i < otherCap; i++){
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
        int cap = mask + 1;
        for(int i = 0; i < cap; i++){
            if(state[i] == LIVE && Objects.equals(e, vals[i])){
                int home   = homeOf(i);
                int offset = (i - home) & mask;
                bitmap[home] &= ~(1 << offset);
                state[i]      = EMPTY;
                vals[i]       = null;
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
        int cap = mask + 1;
        for(int i = 0; i < cap; i++){
            if(state[i] == LIVE && Objects.equals(e, vals[i]))
                return true;
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        int cap = mask + 1;
        for(int i = 0; i < cap; i++){
            if(state[i] == LIVE && Objects.equals(e, vals[i]))
                return new Point(unpackX(keys[i]), unpackY(keys[i]));
        }
        return null;
    }

    @Override
    public void clear(){
        Arrays.fill(state,  EMPTY);
        Arrays.fill(vals,   null);
        Arrays.fill(bitmap, 0);
        size = 0;
    }

    @Override
    public Iterable<Point> points(){
        return () -> new HopIterator<>(){
            @Override protected Point produce(int slot){
                return new Point(unpackX(keys[slot]), unpackY(keys[slot]));
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new HopIterator<Cell<E>>(){
            @Override protected Cell<E> produce(int slot){
                return new Cell<>(unpackX(keys[slot]), unpackY(keys[slot]), (E) vals[slot]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new HopIterator<>(){
            @Override protected E produce(int slot){ return (E) vals[slot]; }
        };
    }

    private abstract class HopIterator<T> implements Iterator<T>{
        private int cursor   = 0;
        private int lastSlot = -1;

        @Override
        public boolean hasNext(){
            int cap = mask + 1;
            while(cursor < cap){
                if(state[cursor] == LIVE) return true;
                cursor++;
            }
            return false;
        }

        @Override
        public T next(){
            int cap = mask + 1;
            while(cursor < cap){
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
            int home   = homeOf(lastSlot);
            int offset = (lastSlot - home) & mask;
            bitmap[home] &= ~(1 << offset);
            state[lastSlot] = EMPTY;
            vals[lastSlot]  = null;
            size--;
            lastSlot = -1;
        }

        protected abstract T produce(int slot);
    }

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator){
        long key  = pack(x, y);
        int  slot = findSlot(key);
        if(slot >= 0){
            vals[slot] = operator.apply((E) vals[slot]);
        } else{
            rawPut(key, operator.apply(null));
        }
    }

    // -------------------------------------------------------------------------
    // Size / conversion
    // -------------------------------------------------------------------------

    public int size(){ return size; }

    public java.util.HashMap<Point, E> toMap(){
        int cap = mask + 1;
        var map = new java.util.HashMap<Point, E>(size * 2);
        for(int i = 0; i < cap; i++){
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
        if(!(obj instanceof HashGrid4<?> other)) return false;
        if(size != other.size) return false;
        int cap = mask + 1;
        for(int i = 0; i < cap; i++){
            if(state[i] != LIVE) continue;
            int j = other.findSlot(keys[i]);
            if(j < 0) return false;
            if(!Objects.equals(vals[i], other.vals[j])) return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        int h   = 0;
        int cap = mask + 1;
        for(int i = 0; i < cap; i++){
            if(state[i] != LIVE) continue;
            long k = keys[i];
            h += (int)(k ^ (k >>> 32)) ^ Objects.hashCode(vals[i]);
        }
        return h;
    }
}
