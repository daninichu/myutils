package com.daninichu.util;

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
	private int size;

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

    public ArrayGrid(ArrayGrid<? extends E> grid){
		data = grid.toArray();
		size = grid.size();
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
			throw new IndexOutOfBoundsException(
					"Point (%d,%d) out of bounds for dimensions (%d,%d)".formatted(x, y, width(), height())
			);
		}
	}

	private static void checkRowInBounds(int y, int height){
		if(y < 0 || height <= y)
			throw new IndexOutOfBoundsException("Row " + y + " out of bounds for height " + height);
	}

	private static void checkColInBounds(int x, int width){
		if(x < 0 || width <= x)
            throw new IndexOutOfBoundsException("Column " + x + " out of bounds for width " + width);
	}

	@Override
	public int size(){
		return size;
	}

	/**
	 * @return {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
    public E get(int x, int y){
		checkInBounds(x, y);
		return (E) data[y][x];
    }

	/**
	 * @return {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
	public E get(Point p){
		return get(p.x, p.y);
	}

	/**
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public E set(int x, int y, E e){
		checkInBounds(x, y);
		E oldValue = (E) data[y][x];
		data[y][x] = Objects.requireNonNull(e);
		if(oldValue == null)
            size++;
		return oldValue;
    }

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
	public E set(Point p, E e){
		return set(p.x, p.y, e);
	}

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If any point in {@code grid} is out of bounds.
	 */
	@Override
	public void setAll(Grid<? extends E> grid){
		for(Cell<? extends E> cell : grid.cells())
            set(cell.x, cell.y, cell.value);
	}

	/**
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public E removePoint(int x, int y){
		checkInBounds(x, y);
        E oldValue = (E) data[y][x];
		data[y][x] = null;
		if(oldValue != null)
			size--;
		return oldValue;
    }

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public E removePoint(Point p){
        return removePoint(p.x, p.y);
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
						size--;
                        return true;
                    }
                }
            }
        }
		return false;
    }

	/**
	 * @return {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public boolean containsPoint(int x, int y){
		checkInBounds(x, y);
		return data[y][x] != null;
    }

	/**
	 * @return {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public boolean containsPoint(Point p){
        return containsPoint(p.x, p.y);
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
		int width = data[0].length;
		for(int y = 0; y < height; y++){
			Object[] row = data[y];
			for(int x = 0; x < width; x++){
				if(row[x] == null){
					row[x] = e;
				}
			}
        }
		size = width * height;
	}

	/**
	 * Sets every point in row {@code y} to {@code e}.
	 * @param y The row index.
	 * @param e The value to fill with.
	 * @throws NullPointerException if {@code e} is null.
	 * @throws IndexOutOfBoundsException if {@code y} is out of bounds.
	 */
	public void fillRow(int y, E e){
		Objects.requireNonNull(e);
		Object[][] data = this.data;
		checkRowInBounds(y, data.length);

		Object[] row = data[y];
		for(int x = 0, width = row.length; x < width; x++){
			if(row[x] == null){
				row[x] = e;
                size++;
            }
		}
	}

	/**
	 * Sets every point in column {@code x} to {@code e}.
	 * @param x The column index.
	 * @param e The value to fill with.
	 * @throws NullPointerException if {@code e} is null.
	 * @throws IndexOutOfBoundsException if {@code x} is out of bounds.
	 */
	public void fillCol(int x, E e){
		Objects.requireNonNull(e);
		Object[][] data = this.data;
		checkColInBounds(x, data[0].length);

		for(int y = 0, height = data.length; y < height; y++){
			Object[] row = data[y];
			if(row[x] == null){
				row[x] = e;
                size++;
            }
		}
	}

	@Override
	public void clear(){
		Object[][] data = this.data;
		int height = data.length;
		int width = data[0].length;
		for(int y = 0; y < height; y++){
			Object[] row = data[y];
			for(int x = 0; x < width; x++){
				if(row[x] != null){
					row[x] = null;
				}
			}
		}
		size = 0;
	}

	/**
	 * Removes all values in row {@code y}, leaving every point vacant.
	 * @param y The row index.
	 * @throws IndexOutOfBoundsException if {@code y} is out of bounds.
	 */
	public void clearRow(int y){
		Object[][] data = this.data;
		checkRowInBounds(y, data.length);

		Object[] row = data[y];
		for(int x = 0, width = row.length; x < width; x++){
			if(row[x] != null){
				row[x] = null;
                size--;
            }
		}
	}

	/**
	 * Removes all values in column {@code x}, leaving every point vacant.
	 * @param x The column index.
	 * @throws IndexOutOfBoundsException if {@code x} is out of bounds.
	 */
	public void clearCol(int x){
		Object[][] data = this.data;
		checkColInBounds(x, data[0].length);

		for(int y = 0, height = data.length; y < height; y++){
			Object[] row = data[y];
			if(row[x] != null){
				row[x] = null;
                size--;
            }
		}
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
			int width = data[y].length;
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
			size--;
			lastY = -1;
		}
	}

	@Override
	public void compute(int x, int y, UnaryOperator<E> operator){
		checkInBounds(x, y);
		Object[] row = data[y];
		E e = (E) row[x];
		E result = operator.apply(e);
		if(e == null){
			if(result != null){
				row[x] = result;
				size++;
			}
		} else if(result == null){
            row[x] = null;
            size--;
        }
	}

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
