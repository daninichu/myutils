package com.daninichu.util;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * A grid with a fixed size. (x,y) coordinates cannot be negative.
 * <p>
 * Backed by a single flat {@code Object[]} in row-major (y-major) order.
 * Index mapping: {@code index = y * width + x}.
 * <p>
 * Null values are not permitted in order to distinguish between vacant and occupied points.
 */
@SuppressWarnings("unchecked")
public class ArrayGrid<E> extends AbstractGrid<E> implements Grid<E>{
    private final Object[] data;
    private final int width, height;

    /**
     * Constructs an empty {@code ArrayGrid} with the given width and height.
     * @param width The width of this grid.
     * @param height The height of this grid.
     * @throws IllegalArgumentException if {@code width} or {@code height} are less than 1.
     */
    public ArrayGrid(int width, int height){
        if(width < 1 || height < 1)
            throw new IllegalArgumentException("width and height must be positive");
        this.width = width;
        this.height = height;
        this.data = new Object[width * height];
    }

    /**
     * Constructs a new {@code ArrayGrid} as a shallow copy of {@code grid}.
     * The new grid has the same dimensions and contains the same element references.
     * @param grid The grid to copy from.
     * @throws NullPointerException if {@code grid} is {@code null}.
     */
    public ArrayGrid(ArrayGrid<? extends E> grid){
        this.width = grid.width;
        this.height = grid.height;
        this.data = Arrays.copyOf(grid.data, grid.data.length);
    }

    /**
     * @return The number of columns in this grid.
     */
    public int width(){
        return width;
    }

    /**
     * @return The number of rows in this grid.
     */
    public int height(){
        return height;
    }

    /**
     * @return The total number of cells in this grid, equal to {@code width() * height()}.
     */
    public int length(){
        return data.length;
    }

    /**
     * Checks if the given point lies within the bounds of this grid.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return {@code true} if {@code 0 <= x < width()} and {@code 0 <= y < height()}.
     */
    public boolean inBounds(int x, int y){
        return 0 <= x && x < width && 0 <= y && y < height;
    }

    /**
     * Checks if the given point lies within the bounds of this grid.
     * @param p The point to check.
     * @return {@code true} if {@code p} is within the bounds of this grid.
     * @throws NullPointerException if {@code p} is null.
     */
    public boolean inBounds(Point p){
        return inBounds(p.x, p.y);
    }

    private void checkInBounds(int x, int y){
        if(!inBounds(x, y)){
            throw new IndexOutOfBoundsException("Point (" + x + ',' + y +
                    ") out of bounds for dimensions " + width + 'x' + height);
        }
    }

    private int index(int x, int y){
        return y * width + x;
    }

    @Override
    public E get(int x, int y){
        checkInBounds(x, y);
        return (E) data[index(x, y)];
    }

    /**
     * Retrieves the value at the given flat index, where {@code index = y * width() + x}.
     * This method does not perform a 2D bounds check.
     * @param index The flat index in row-major (y-major) order.
     * @return The value at the given index, or null if there is none.
     * @throws ArrayIndexOutOfBoundsException if {@code index < 0 || index >= length()})
     */
    public E get(int index){
        return (E) data[index];
    }

    @Override
    public E set(int x, int y, E e){
        checkInBounds(x, y);
        int index = index(x, y);
        E oldValue = (E) data[index];
        data[index] = Objects.requireNonNull(e);
        return oldValue;
    }

    /**
     * Overwrites the value at the given flat index, where {@code index = y * width() + x}.
     * This method does not perform a 2D bounds check.
     * @param index The flat index in row-major (y-major) order.
     * @param e The new value.
     * @return The value previously at the given index, or null if there was none.
     * @throws ArrayIndexOutOfBoundsException if {@code index < 0 || index >= length()})
     * @throws NullPointerException if {@code e} is null.
     */
    public E set(int index, E e){
        E oldValue = (E) data[index];
        data[index] = Objects.requireNonNull(e);
        return oldValue;
    }

    @Override
    public E getOrSet(int x, int y, E e){
        checkInBounds(x, y);
        int index = index(x, y);
        E oldValue = (E) data[index];
        if(oldValue == null){
            data[index] = Objects.requireNonNull(e);
        }
        return oldValue;
    }

    /**
     * Returns the value at the given flat index,
     * or sets and returns the given value if the index is vacant,
     * where {@code index = y * width() + x}.
     * This method does not perform a 2D bounds check.
     * @param index The flat index in row-major (y-major) order.
     * @param e The value to set if the index is vacant.
     * @return The existing value at the given index, or {@code e} if the index was vacant.
     * @throws IndexOutOfBoundsException if the index is out of bounds of this grid.
     * @throws NullPointerException if the index is vacant
     * 		   and {@code e} is null and this grid does not permit null values.
     */
    public E getOrSet(int index, E e){
        E oldValue = (E) data[index];
        if(oldValue == null){
            data[index] = Objects.requireNonNull(e);
        }
        return oldValue;
    }

    @Override
    public E compute(int x, int y, Function<? super E, ? extends E> function){
        checkInBounds(x, y);
        int index = index(x, y);
        E e = function.apply((E) data[index]);
        data[index] = e;
        return e;
    }

    /**
     * Replaces the value at the given flat index with the result of applying {@code function} to it,
     * where {@code index = y * width() + x}.
     * The function receives null if there is no value at the index.
     * If the function returns null, the value at the index is removed.
     * This method does not perform a 2D bounds check.
     * @param index The flat index in row-major (y-major) order.
     * @param function The function to apply to the current value.
     * @return The new value at the given index, or null if the index is now vacant.
     * @throws ArrayIndexOutOfBoundsException if {@code index < 0 || index >= length()})
     * @throws NullPointerException if {@code function} is null.
     */
    public E compute(int index, Function<? super E, ? extends E> function){
        E e = function.apply((E) data[index]);
        data[index] = e;
        return e;
    }

    @Override
    public E removePoint(int x, int y){
        checkInBounds(x, y);
        int index = index(x, y);
        E e = (E) data[index];
        data[index] = null;
        return e;
    }

    /**
     * Removes the value at the given flat index, where {@code index = y * width() + x}.
     * This method does not perform a 2D bounds check.
     * @param index The flat index in row-major (y-major) order.
     * @return The value previously at the given index, or null if there was none.
     * @throws ArrayIndexOutOfBoundsException if {@code index < 0 || index >= length()})
     */
    public E removePoint(int index){
        E e = (E) data[index];
        data[index] = null;
        return e;
    }

    @Override
    public boolean containsPoint(int x, int y){
        checkInBounds(x, y);
        return data[index(x, y)] != null;
    }

    /**
     * Checks if there exists a value at the given flat index, where {@code index = y * width() + x}.
     * This method does not perform a 2D bounds check.
     * @param index The flat index in row-major (y-major) order.
     * @return {@code true} if there is a value.
     * @throws ArrayIndexOutOfBoundsException if {@code index < 0 || index >= length()})
     */
    public boolean containsPoint(int index){
        return data[index] != null;
    }

    @Override
    public boolean removeValue(E e){
        if(e != null){
            Object[] data = this.data;
            for(int i = 0, n = data.length; i < n; i++){
                Object o = data[i];
                if(o != null && e.equals(o)){
                    data[i] = null;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(E e){
        if(e != null){
            Object[] data = this.data;
            for(int i = 0, n = data.length; i < n; i++){
                Object o = data[i];
                if(o != null && e.equals(o))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Point pointOf(E e){
        if(e != null){
            Object[] data = this.data;
            for(int i = 0, n = data.length; i < n; i++){
                Object o = data[i];
                if(o != null && e.equals(o))
                    return new Point(i % width, i / width);
            }
        }
        return null;
    }

    /**
     * Sets every point in this grid to the given value.
     * @param e The value to fill this grid with.
     * @throws NullPointerException if {@code e} is null.
     */
    public void fill(E e){
        Arrays.fill(data, Objects.requireNonNull(e));
    }

    @Override
    public void clear(){
        Arrays.fill(data, null);
    }

    @Override
    public Iterable<Point> points(){
        return () -> new ArrayGridIterator<Point>(){
            @Override
            public Point next(){
                if(!hasNext())
                    throw new NoSuchElementException();
                return new Point(index % width, (lastIndex = index++) / width);
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new ArrayGridIterator<Cell<E>>(){
            @Override
            public Cell<E> next(){
                if(!hasNext())
                    throw new NoSuchElementException();
                return new Cell<>(index % width, index / width, (E) data[lastIndex = index++]);
            }
        };
    }

    @Override
    public Iterator<E> iterator(){
        return new ArrayGridIterator<E>(){
            @Override
            public E next(){
                if(!hasNext())
                    throw new NoSuchElementException();
                return (E) data[lastIndex = index++];
            }
        };
    }

    private abstract class ArrayGridIterator<T> implements Iterator<T>{
        int index, lastIndex = -1;

        @Override
        public boolean hasNext(){
            Object[] data = ArrayGrid.this.data;
            while(index < data.length){
                if(data[index] != null)
                    return true;
                index++;
            }
            return false;
        }

        @Override
        public void remove(){
            if(lastIndex == -1)
                throw new IllegalStateException();
            data[lastIndex] = null;
            lastIndex = -1;
        }
    }

    @Override
    public boolean equals(Object obj){
        return this == obj || obj instanceof ArrayGrid<?> g
                && width == g.width && Arrays.equals(data, g.data);
    }

    @Override
    public int hashCode(){
        return 31 * Arrays.hashCode(data) + width;
    }
}
