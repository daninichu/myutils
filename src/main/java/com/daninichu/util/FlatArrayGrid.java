package com.daninichu.util;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A grid with a fixed size. (x,y) coordinates cannot be negative.
 * <p>
 * Backed by a single flat {@code Object[]} in row-major (y-major) order.
 * Index mapping: {@code index = y * width + x}.
 * <p>
 * Null values are not permitted in order to distinguish between vacant and occupied points.
 */
@SuppressWarnings("unchecked")
public class FlatArrayGrid<E> extends AbstractGrid<E> implements Grid<E>{
    private final Object[] data;
    private final int width, height;

    /**
     * Constructs an empty {@code ArrayGrid} with the given width and height.
     * @param width The width of this grid.
     * @param height The height of this grid.
     * @throws IllegalArgumentException if {@code width} or {@code height} are less than 1.
     */
    public FlatArrayGrid(int width, int height){
        if(width < 1 || height < 1)
            throw new IllegalArgumentException("width and height must be positive");
        this.width = width;
        this.height = height;
        this.data = new Object[width * height];
    }

    /**
     * Creates a shallow copy of {@code grid}.
     * The new grid has the same dimensions and contains the same element references.
     * @param grid The grid to copy from.
     * @throws NullPointerException if {@code grid} is {@code null}.
     */
    public FlatArrayGrid(FlatArrayGrid<? extends E> grid){
        this.width = grid.width;
        this.height = grid.height;
        this.data = Arrays.copyOf(grid.data, grid.data.length);
    }

    public int width(){
        return width;
    }

    public int height(){
        return height;
    }

    public int length(){
        return data.length;
    }

    public boolean inBounds(int x, int y){
        return 0 <= x && x < width && 0 <= y && y < height;
    }

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

    public E set(int index, E e){
        E oldValue = (E) data[index];
        data[index] = Objects.requireNonNull(e);
        return oldValue;
    }

    @Override
    public E removePoint(int x, int y){
        checkInBounds(x, y);
        int index = index(x, y);
        E e = (E) data[index];
        data[index] = null;
        return e;
    }

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
            Object[] data = FlatArrayGrid.this.data;
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
    public void compute(int x, int y, UnaryOperator<E> operator){
        checkInBounds(x, y);
        int index = index(x, y);
        data[index] = operator.apply((E) data[index]);
    }

    public void compute(int index, UnaryOperator<E> operator){
        data[index] = operator.apply((E) data[index]);
    }

    /**
     * Returns a copy of the backing flat array.
     * Index mapping: {@code index = y * width + x}.
     */
    public E[] toArray(){
        return (E[]) Arrays.copyOf(data, data.length);
    }

    @Override
    public boolean equals(Object obj){
        return this == obj || obj instanceof FlatArrayGrid<?> g
                && width == g.width && Arrays.equals(data, g.data);
    }

    @Override
    public int hashCode(){
        return 31 * Arrays.hashCode(data) + width;
    }
}
