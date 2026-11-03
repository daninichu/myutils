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
 * The internal 2D array of this class is row-major (y-major).
 * <p>
 * Null values are not permitted in order to distinguish between vacant and occupied points.
 */
@SuppressWarnings("unchecked")
public class ArrayGrid<E> extends AbstractGrid<E> implements Grid<E>{
	private final Object[][] data;

	/**
	 * Constructs an empty {@code ArrayGrid} with the given width and height.
	 * @param width The width of this grid.
	 * @param height The height of this grid.
	 * @throws IllegalArgumentException if {@code width} or {@code height} are less than 1.
	 */
    public ArrayGrid(int width, int height){
		if(width < 1 || height < 1)
            throw new IllegalArgumentException("width and height must be 1 or greater");
        data = new Object[height][width];
    }

	/**
	 * Creates a shallow copy of {@code grid}.
	 * The new grid has the same dimensions and contains the same element references.
	 * @param grid The grid to copy from.
	 * @throws NullPointerException if {@code grid} is {@code null}.
	 */
    public ArrayGrid(ArrayGrid<? extends E> grid){
		data = grid.toArray();
    }

	/**
	 * @return The number of columns in this grid.
	 */
	public int width(){
		return data[0].length;
	}

	/**
	 * @return The number of rows in this grid.
	 */
	public int height(){
		return data.length;
	}

	/**
	 * Checks whether the given point is within the bounds of this grid.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @return {@code true} if {@code 0 <= x < width()} and {@code 0 <= y < height()}.
	 */
	public boolean inBounds(int x, int y){
		Object[][] data = this.data;
		return 0 <= x && x < data[0].length && 0 <= y && y < data.length;
	}

	/**
	 * Checks whether the given point is within the bounds of this grid.
	 * @param p The point to check.
	 * @return {@code true} if {@code p} is within bounds.
	 * @throws NullPointerException if {@code p} is null.
	 */
	public boolean inBounds(Point p){
		return inBounds(p.x, p.y);
	}

	private void checkInBounds(int x, int y){
		if(!inBounds(x, y)){
			throw new IndexOutOfBoundsException("Point (" + x + "," + y
					+ ") out of bounds for dimensions " + width() + "x" + height()
			);
		}
	}

	@Override
    public E get(int x, int y){
		checkInBounds(x, y);
		return (E) data[y][x];
    }

    @Override
    public E set(int x, int y, E e){
		checkInBounds(x, y);
		E oldValue = (E) data[y][x];
		data[y][x] = Objects.requireNonNull(e);
		return oldValue;
    }

    @Override
    public E removePoint(int x, int y){
		checkInBounds(x, y);
        E oldValue = (E) data[y][x];
		data[y][x] = null;
		return oldValue;
    }

	@Override
	public boolean removeValue(E e){
        if(e != null){
			Object[][] data = this.data;
			int height = data.length;
			int width = data[0].length;
			for(int y = 0; y < height; y++){
				Object[] row = data[y];
				for(int x = 0; x < width; x++){
					Object o = row[x];
                    if(o != null && e.equals(o)){
                        row[x] = null;
                        return true;
                    }
                }
            }
        }
		return false;
    }

    @Override
    public boolean containsPoint(int x, int y){
		checkInBounds(x, y);
		return data[y][x] != null;
    }

	@Override
	public boolean containsValue(E e){
		if(e != null){
			Object[][] data = this.data;
			int height = data.length;
			int width = data[0].length;
			for(int y = 0; y < height; y++){
				Object[] row = data[y];
				for(int x = 0; x < width; x++){
					Object o = row[x];
					if(o != null && e.equals(o))
                        return true;
				}
			}
		}
		return false;
	}

	@Override
	public Point pointOf(E e){
		if(e != null){
			Object[][] data = this.data;
			int height = data.length;
			int width = data[0].length;
			for(int y = 0; y < height; y++){
				Object[] row = data[y];
				for(int x = 0; x < width; x++){
					Object o = row[x];
                    if(o != null && e.equals(o))
                        return new Point(x, y);
				}
			}
		}
		return null;
	}

	/**
	 * Sets every point in this grid to {@code e}.
	 * @param e The value to fill with.
	 * @throws NullPointerException if {@code e} is null.
	 */
	public void fill(E e){
		Objects.requireNonNull(e);
		Object[][] data = this.data;
		int height = data.length;
		for(int y = 0; y < height; y++){
			Arrays.fill(data[y], e);
        }
	}

	@Override
	public void clear(){
		Object[][] data = this.data;
		for(int y = 0, height = data.length; y < height; y++)
            Arrays.fill(data[y], null);
	}

	@Override
	public Iterable<Point> points(){
		return () -> new ArrayGridIterator<Point>(){
			@Override
			public Point next(){
				if(!hasNext())
					throw new NoSuchElementException();
				return new Point(lastX = x++, lastY = y);
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
                return new Cell<>(x, y, (E) data[lastY = y][lastX = x++]);
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
                return (E) data[lastY = y][lastX = x++];
            }
		};
	}

    private abstract class ArrayGridIterator<T> implements Iterator<T>{
		int x, y, lastX, lastY = -1;

		@Override
		public boolean hasNext(){
			Object[][] data = ArrayGrid.this.data;
			int height = data.length;
			int width = data[0].length;
			while(y < height){
				Object[] row = data[y];
				while(x < width){
					if(row[x] != null)
                        return true;
					x++;
				}
				x = 0;
				y++;
			}
			return false;
		}

		@Override
		public void remove(){
			if(lastY == -1)
				throw new IllegalStateException();
			data[lastY][lastX] = null;
			lastY = -1;
		}
	}

	@Override
	public void compute(int x, int y, UnaryOperator<E> operator){
		checkInBounds(x, y);
		Object[] row = data[y];
		row[x] = operator.apply((E) row[x]);
	}

	/**
	 * Returns a shallow copy of the internal data as a new 2D array.
	 * Vacant points are represented as {@code null}.
	 * <p>
	 * The array is row-major (y-major). {@code result[y][x]} holds the value at point (x,y).
	 * @return A new {@code E[][]} of dimensions {@code [height()][width()]}.
	 */
	public E[][] toArray(){
		Object[][] src = data;
		int height = src.length;
		int width = src[0].length;
		Object[][] dst = new Object[height][];
		for(int y = 0; y < height; y++){
			dst[y] = Arrays.copyOf(src[y], width);
		}
		return (E[][]) dst;
	}

	@Override
	public boolean equals(Object obj){
        return this == obj || obj instanceof ArrayGrid<?> other && Arrays.deepEquals(data, other.data);
    }

	@Override
	public int hashCode(){
		return Arrays.deepHashCode(data);
	}
}
