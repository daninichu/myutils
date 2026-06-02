package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A hash-based Grid backed by a Swiss Table (SWAR) open-addressing hash table.
 * <p>
 * <h3>Design</h3>
 * Each slot has a 1-byte control byte stored in a flat {@code byte[]} parallel to the key/value
 * arrays. Control bytes have three states:
 * <ul>
 *   <li>{@code EMPTY   = 0xFF} — slot has never been written.</li>
 *   <li>{@code DELETED = 0x80} — slot was occupied but has been deleted (tombstone).</li>
 *   <li>{@code 0x00..0x7F} — slot is live; the byte holds the low 7 bits of the key's hash
 *       (the "H2 fingerprint").</li>
 * </ul>
 *
 * <h3>Group probing (SWAR)</h3>
 * Control bytes are probed in groups of {@value #GROUP} using {@code long} bitmask arithmetic
 * that mimics SIMD: a single {@code matchByte} call checks all 8 bytes in a group simultaneously,
 * returning a bitmask with bit 7 set for each matching slot. This typically resolves a lookup
 * in one or two group loads with zero full-key comparisons for absent keys.
 * <p>
 * The table capacity is always a multiple of {@value #GROUP} so group reads never straddle
 * a capacity boundary.
 *
 * <h3>Two-level hashing</h3>
 * A 64-bit hash {@code h} is split into:
 * <ul>
 *   <li>{@code H1 = h >>> 7} — selects the starting group index.</li>
 *   <li>{@code H2 = h & 0x7F} — the fingerprint written into the control byte on insertion.</li>
 * </ul>
 *
 * <h3>Load factor</h3>
 * Capped at 0.875 (7/8 of capacity). Swiss Tables tolerate high load because the fingerprint
 * filter eliminates most full-key comparisons before they happen.
 * <p>
 * Null values are permitted.
 */
@SuppressWarnings("unchecked")
public class HashGrid4<E> extends AbstractGrid<E> implements Grid<E>{

    // -------------------------------------------------------------------------
    // Control byte constants
    // -------------------------------------------------------------------------

    private static final byte EMPTY   = (byte) 0xFF;
    private static final byte DELETED = (byte) 0x80;

    // -------------------------------------------------------------------------
    // SWAR constants (operate on 8 control bytes packed into a long, little-endian)
    // -------------------------------------------------------------------------

    /** One copy of a byte value broadcast across all 8 bytes of a long. */
    private static final long BROADCAST  = 0x0101_0101_0101_0101L;
    /** Bit 7 of every byte. */
    private static final long SIGN_BITS  = 0x8080_8080_8080_8080L;

    // -------------------------------------------------------------------------
    // Group size
    // -------------------------------------------------------------------------

    /** Number of slots per group — must equal 8 (one long). */
    private static final int GROUP      = 8;
    private static final int GROUP_MASK = GROUP - 1;

    // -------------------------------------------------------------------------
    // Sizing
    // -------------------------------------------------------------------------

    private static final int   DEFAULT_GROUPS = 4;          // 32 slots
    private static final float LOAD_FACTOR    = 0.875f;     // 7/8

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Flat control byte array. Length is always a multiple of GROUP. */
    private byte[]   ctrl;
    private long[]   keys;
    private Object[] vals;
    private int      numGroups;
    private int      size;
    private int      threshold;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public HashGrid4(){
        init(DEFAULT_GROUPS);
    }

    public HashGrid4(int initialCapacity){
        init(groupsFor(initialCapacity));
    }

    public HashGrid4(Grid<? extends E> grid){
        init(DEFAULT_GROUPS);
        setAll(grid);
    }

    private void init(int groups){
        numGroups = groups;
        int cap   = groups * GROUP;
        ctrl      = new byte[cap];
        keys      = new long[cap];
        vals      = new Object[cap];
        Arrays.fill(ctrl, EMPTY);
        threshold = (int)(cap * LOAD_FACTOR);
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
    // Hashing & fingerprint split
    // -------------------------------------------------------------------------

    private static long hash(long key){
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return key;
    }

    /** H1: selects the starting slot (group-aligned). */
    private int h1(long h){ return (int)(h >>> 7) & (capacity() - 1); }

    /** H2: 7-bit fingerprint stored in the control byte. Always in 0x00..0x7F. */
    private static byte h2(long h){ return (byte)(h & 0x7F); }

    private int capacity(){ return numGroups * GROUP; }

    // -------------------------------------------------------------------------
    // SWAR group operations
    // -------------------------------------------------------------------------

    /**
     * Loads 8 control bytes starting at {@code groupStart} as a little-endian long.
     */
    private long loadGroup(int groupStart){
        long v = 0;
        byte[] c = ctrl;
        for(int i = 0; i < GROUP; i++)
            v |= (c[groupStart + i] & 0xFFL) << (i * 8);
        return v;
    }

    /**
     * Returns a bitmask with bit 7 set at each byte position where the control byte equals {@code b}.
     * All other bits are 0.
     */
    private static long matchByte(long group, int b){
        long x = group ^ (BROADCAST * (b & 0xFFL));
        return (x - BROADCAST) & ~x & SIGN_BITS;
    }

    /**
     * Returns a bitmask with bit 7 set at each byte position where the control byte is EMPTY.
     */
    private static long matchEmpty(long group){
        // EMPTY = 0xFF. Bit 7 is set and bits 0-6 are all set.
        // A slot is empty iff (byte & 0x80) != 0 AND (byte & 0x7F) == 0x7F.
        return group & (group << 1) & SIGN_BITS;
    }

    /**
     * Returns a bitmask with bit 7 set at each byte position where the slot is empty or deleted.
     */
    private static long matchEmptyOrDeleted(long group){
        // EMPTY = 0xFF (bit7=1, bit6=1), DELETED = 0x80 (bit7=1, bit6=0), LIVE = 0x00..0x7F (bit7=0).
        return group & SIGN_BITS;
    }

    /**
     * Extracts the index (0-7) of the lowest set bit-7 from a match bitmask, or -1 if none.
     */
    private static int firstMatch(long mask){
        if(mask == 0) return -1;
        return Long.numberOfTrailingZeros(mask) >>> 3;
    }

    /**
     * Clears the lowest set bit-7 from a match bitmask (advances to next match).
     */
    private static long clearFirst(long mask){
        return mask & (mask - 1);
    }

    // -------------------------------------------------------------------------
    // Core lookup
    // -------------------------------------------------------------------------

    /**
     * Returns the slot index of {@code key}, or -1 if absent.
     */
    private int findSlot(long key){
        long h   = hash(key);
        byte fp  = h2(h);
        int  cap = capacity();
        int  i   = h1(h) & ~GROUP_MASK;    // align to group boundary

        while(true){
            long group = loadGroup(i);
            long hits  = matchByte(group, fp);
            while(hits != 0){
                int offset = firstMatch(hits);
                int slot   = (i + offset) & (cap - 1);
                if(keys[slot] == key) return slot;
                hits = clearFirst(hits);
            }
            if(firstMatch(matchEmpty(group)) >= 0) return -1;
            i = (i + GROUP) & (cap - 1);
        }
    }

    // -------------------------------------------------------------------------
    // Insertion
    // -------------------------------------------------------------------------

    private E rawPut(long key, E value){
        if(size >= threshold){
            resize();
        }

        long h   = hash(key);
        byte fp  = h2(h);
        int  cap = capacity();
        int  i   = h1(h) & ~GROUP_MASK;
        int  insertSlot = -1;

        while(true){
            long group = loadGroup(i);

            // Check for existing key or a free slot to insert into.
            long hits = matchByte(group, fp);
            while(hits != 0){
                int offset = firstMatch(hits);
                int slot   = (i + offset) & (cap - 1);
                if(keys[slot] == key){
                    E old    = (E) vals[slot];
                    vals[slot] = value;
                    return old;
                }
                hits = clearFirst(hits);
            }

            // Record first available slot (empty or deleted) for insertion.
            if(insertSlot < 0){
                long avail = matchEmptyOrDeleted(group);
                if(avail != 0){
                    int offset = firstMatch(avail);
                    insertSlot = (i + offset) & (cap - 1);
                }
            }

            // If this group has an empty slot, the key is definitely not in the table.
            if(firstMatch(matchEmpty(group)) >= 0){
                ctrl[insertSlot] = fp;
                keys[insertSlot] = key;
                vals[insertSlot] = value;
                size++;
                return null;
            }

            i = (i + GROUP) & (cap - 1);
        }
    }

    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    private E rawRemove(long key){
        int slot = findSlot(key);
        if(slot < 0) return null;
        E old       = (E) vals[slot];
        ctrl[slot]  = DELETED;
        vals[slot]  = null;
        size--;
        return old;
    }

    // -------------------------------------------------------------------------
    // Resize
    // -------------------------------------------------------------------------

    private void resize(){
        byte[]   oldCtrl = ctrl;
        long[]   oldKeys = keys;
        Object[] oldVals = vals;
        int      oldCap  = capacity();
        init(numGroups * 2);
        for(int i = 0; i < oldCap; i++){
            if(oldCtrl[i] != EMPTY && oldCtrl[i] != DELETED)
                rawPut(oldKeys[i], (E) oldVals[i]);
        }
    }

    private static int groupsFor(int capacity){
        int groups = Math.max(DEFAULT_GROUPS, Integer.highestOneBit((int) Math.ceil(capacity / LOAD_FACTOR)));
        return groups < capacity ? groups * 2 : groups;
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
            int otherCap = other.capacity();
            for(int i = 0; i < otherCap; i++){
                if(other.ctrl[i] != EMPTY && other.ctrl[i] != DELETED)
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
        int cap = capacity();
        for(int i = 0; i < cap; i++){
            if(ctrl[i] != EMPTY && ctrl[i] != DELETED && Objects.equals(e, vals[i])){
                ctrl[i] = DELETED;
                vals[i] = null;
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
        int cap = capacity();
        for(int i = 0; i < cap; i++){
            if(ctrl[i] != EMPTY && ctrl[i] != DELETED && Objects.equals(e, vals[i]))
                return true;
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        int cap = capacity();
        for(int i = 0; i < cap; i++){
            if(ctrl[i] != EMPTY && ctrl[i] != DELETED && Objects.equals(e, vals[i]))
                return new Point(unpackX(keys[i]), unpackY(keys[i]));
        }
        return null;
    }

    @Override
    public void clear(){
        Arrays.fill(ctrl, EMPTY);
        Arrays.fill(vals, null);
        size = 0;
    }

    @Override
    public Iterable<Point> points(){
        return () -> new SwissIterator<>(){
            @Override
            protected Point produce(int slot){
                return new Point(unpackX(keys[slot]), unpackY(keys[slot]));
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new SwissIterator<Cell<E>>(){
            @Override
            protected Cell<E> produce(int slot){
                return new Cell<>(unpackX(keys[slot]), unpackY(keys[slot]), (E) vals[slot]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new SwissIterator<>(){
            @Override
            protected E produce(int slot){
                return (E) vals[slot];
            }
        };
    }

    private abstract class SwissIterator<T> implements Iterator<T>{
        private int cursor   = 0;
        private int lastSlot = -1;

        @Override
        public boolean hasNext(){
            int cap = capacity();
            while(cursor < cap){
                if(ctrl[cursor] != EMPTY && ctrl[cursor] != DELETED) return true;
                cursor++;
            }
            return false;
        }

        @Override
        public T next(){
            int cap = capacity();
            while(cursor < cap){
                if(ctrl[cursor] != EMPTY && ctrl[cursor] != DELETED){
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
            ctrl[lastSlot] = DELETED;
            vals[lastSlot] = null;
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
        int cap = capacity();
        var map = new java.util.HashMap<Point, E>(size * 2);
        for(int i = 0; i < cap; i++){
            if(ctrl[i] != EMPTY && ctrl[i] != DELETED)
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
        int cap = capacity();
        for(int i = 0; i < cap; i++){
            if(ctrl[i] == EMPTY || ctrl[i] == DELETED) continue;
            int j = other.findSlot(keys[i]);
            if(j < 0) return false;
            if(!Objects.equals(vals[i], other.vals[j])) return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        int h   = 0;
        int cap = capacity();
        for(int i = 0; i < cap; i++){
            if(ctrl[i] == EMPTY || ctrl[i] == DELETED) continue;
            long k = keys[i];
            h += (int)(k ^ (k >>> 32)) ^ Objects.hashCode(vals[i]);
        }
        return h;
    }
}
