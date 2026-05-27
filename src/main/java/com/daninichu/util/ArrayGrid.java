package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A grid with a fixed size. (x,y) coordinates cannot be negative.
 * <p>
 * The internal 2D array of this class is y-major.
 */
@SuppressWarnings("unchecked")
public class ArrayGrid<E> extends AbstractGrid<E> implements Grid<E> {
	private final Object[][] data;

	/**
	 * Sets the dimensions of the grid.
	 * @param width
	 * @param height
	 * @throws IllegalArgumentException if {@code width} or {@code height} are less than 1.
	 */
    public ArrayGrid(int width, int height){
		if(width < 1 || height < 1)
            throw new IllegalArgumentException("width and height must be 1 or greater");
        this.data = new Object[height][width];
    }

    public ArrayGrid(ArrayGrid<? extends E> grid){
		this.data = grid.toArray();
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
    public void set(int x, int y, E e){
		checkInBounds(x, y);
		data[y][x] = Objects.requireNonNull(e);
    }

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
	public void set(Point p, E e){
		set(p.x, p.y, e);
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
    public void removePoint(int x, int y){
		checkInBounds(x, y);
		data[y][x] = null;
    }

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public void removePoint(Point p){
        removePoint(p.x, p.y);
    }

	@Override
	public void removeValue(E e){
        if(e != null){
			for(int y = 0; y < height(); y++){
				Object[] inner = data[y];
				for(int x = 0; x < inner.length; x++){
					Object value = inner[x];
					if(value != null){
						if(e.equals(value)){
							data[y][x] = null;
                            return;
                        }
                    }
                }
            }
        }
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
		if(e != null)
            for(E o : this)
                if(e.equals(o))
                    return true;
		return false;
	}

	public void fill(E e){
		for(Object[] inner : data)
			Arrays.fill(inner, e);
	}

	public void fillRow(int y, E e){
		Arrays.fill(data[y], e);
	}

	public void fillCol(int x, E e){
		for(int y = 0, height = height(); y < height; y++){
			data[y][x] = e;
		}
	}

	@Override
	public void clear(){
        for(Object[] inner : data)
            Arrays.fill(inner, null);
	}

	public void clearRow(int y){
		Arrays.fill(data[y], null);
	}

	public void clearCol(int x){
		for(int y = 0, height = height(); y < height; y++){
			data[y][x] = null;
		}
	}

	@Override
	public Iterable<Point> points(){
		return () -> new ArrayGridIterator<Point>(){
			@Override
			public Point next(){
				if(hasNext())
					return new Point(lastX = x++, lastY = y);
				throw new NoSuchElementException();
			}
		};
	}

    @Override
    public Iterable<Cell<E>> cells(){
		return () -> new ArrayGridIterator<Cell<E>>(){
			@Override
			public Cell<E> next(){
				if(hasNext())
					return new Cell<>(x, y, (E) data[lastY = y][lastX = x++]);
				throw new NoSuchElementException();
			}
		};
	}

    @Override
    public Iterator<E> iterator(){
		return new ArrayGridIterator<E>(){
			@Override
			public E next(){
				if(hasNext())
                    return (E) data[lastY = y][lastX = x++];
				throw new NoSuchElementException();
			}
		};
	}

    private abstract class ArrayGridIterator<T> implements Iterator<T> {
		int x, y, lastX, lastY = -1;

		@Override
		public boolean hasNext(){
			int height = height();
			int width = width();
			while(y < height){
				Object[] inner = data[y];
				while(x < width){
					if(inner[x] != null)
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
		data[y][x] = operator.apply((E) data[y][x]);
	}

	public E[][] toArray(){
		int height = height();
		Object[][] array = new Object[height][];
		int width = width();
		for(int y = 0; y < height; y++){
			array[y] = Arrays.copyOf(data[y], width);
		}
		return (E[][]) array;
	}

	@Override
	public boolean equals(Object obj){
        return this == obj || obj instanceof ArrayGrid<?> arrayGrid && Arrays.deepEquals(data, arrayGrid.data);
    }
}
