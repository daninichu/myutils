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
	 * Sets the dimensions of the grid.
	 * @param width
	 * @param height
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

	public int width(){
		return data[0].length;
	}

	public int height(){
		return data.length;
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean inBounds(int x, int y){
		return 0 <= x && x < width() && 0 <= y && y < height();
	}

	/**
	 *
	 * @param p
	 * @return
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
            for(E o : this){
                if(e.equals(o))
                    return true;
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
                    if(o != null && e.equals(o)){
                        return new Point(x, y);
                    }
				}
			}
		}
		return null;
	}

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

	public void fillRow(int y, E e){
		Objects.requireNonNull(e);
		Object[] row = data[y];
		for(int x = 0, width = row.length; x < width; x++){
			if(row[x] == null){
				row[x] = e;
                size++;
            }
		}
	}

	public void fillCol(int x, E e){
		Objects.requireNonNull(e);
		Object[][] data = this.data;
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

	public void clearRow(int y){
		Object[] row = data[y];
		for(int x = 0, width = row.length; x < width; x++){
			if(row[x] != null){
				row[x] = null;
                size--;
            }
		}
	}

	public void clearCol(int x){
		Object[][] data = this.data;
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
			int height = height();
			int width = width();
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
		int height = height();
		int width = width();
		Object[][] src = this.data;
		Object[][] dst = new Object[height][];
		for(int y = 0; y < height; y++){
			dst[y] = Arrays.copyOf(src[y], width);
		}
		return (E[][]) dst;
	}

	@Override
	public boolean equals(Object obj){
        return this == obj || obj instanceof ArrayGrid<?> arrayGrid && Arrays.deepEquals(data, arrayGrid.data);
    }

	@Override
	public int hashCode(){
		return Arrays.deepHashCode(data);
	}
}
