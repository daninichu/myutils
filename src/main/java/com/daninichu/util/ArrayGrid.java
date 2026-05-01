package com.daninichu.util;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

/**
 * A grid with a fixed size. (x, y) coordinates cannot be negative.
 */
@SuppressWarnings("unchecked")
public class ArrayGrid<E> implements Grid<E> {
	private final Object[][] data;

	/**
	 * Sets the dimensions of the grid.
	 * @param width
	 * @param height
	 * @throws IllegalArgumentException if {@code width} or {@code height} are less than 1.
	 */
    public ArrayGrid(int width, int height) {
		if (width < 1 || height < 1)
            throw new IllegalArgumentException("width and height must be 1 or greater");
        this.data = new Object[width][height];
    }

    public ArrayGrid(ArrayGrid<E> grid) {
		this.data = new Object[grid.width()][];
		for (int x = 0; x < grid.width(); x++) {
			data[x] = Arrays.copyOf(grid.data[x], grid.height());
		}
    }

	public int width() {
		return data.length;
	}
	
	public int height() {
		return data[0].length;
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean inBounds(int x, int y) {
		return 0 <= x && x < width() && 0 <= y && y < height();
	}

	/**
	 *
	 * @param p
	 * @return
	 * @throws NullPointerException if {@code p} is null.
	 */
	public boolean inBounds(Point p) {
		return 0 <= p.x && p.x < width() && 0 <= p.y && p.y < height();
	}

	private void checkInBounds(int x, int y) {
		if (!inBounds(x, y)) {
			throw new IndexOutOfBoundsException(
				"(%d, %d) out of bounds for dimensions (%d, %d)".formatted(x, y, width(), height())
			);
		}
	}

	public void fill(E e){
        for(Object[] inner : data)
            Arrays.fill(inner, e);
	}

	/**
	 * @return {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
    public E get(int x, int y) {
		checkInBounds(x, y);
		return (E) data[x][y];
    }

	/**
	 * @return {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
	public E get(Point p) {
		return get(p.x, p.y);
	}

	/**
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public void set(int x, int y, E e) {
		checkInBounds(x, y);
		data[x][y] = e;
    }

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
	@Override
	public void set(Point p, E e) {
		set(p.x, p.y, e);
	}

	/**
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public void remove(int x, int y) {
		checkInBounds(x, y);
        data[x][y] = null;
    }

	/**
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public void remove(Point p) {
        remove(p.x, p.y);
    }

	/**
	 * @return {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public boolean containsPoint(int x, int y) {
		checkInBounds(x, y);
        return data[x][y] != null;
    }

	/**
	 * @return {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException If the point is out of bounds.
	 */
    @Override
    public boolean containsPoint(Point p) {
        return containsPoint(p.x, p.y);
    }

	@Override
	public boolean containsValue(E e){
		if(e != null){
            for(Object[] inner : data){
                for(Object o : inner){
                    if(o.equals(e)){
                        return true;
                    }
                }
            }
        }
		return false;
	}

	@Override
	public void clear() {
        for(Object[] inner : data)
            Arrays.fill(inner, null);
	}

	@Override
	public Iterable<Point> points() {
		return () -> new ArrayGridIterator<Point>(){
			@Override
			public Point next() {
				if(hasNext())
					return new Point(x, y++);
				throw new NoSuchElementException();
			}
		};
	}

    @Override
    public Iterable<Cell<E>> cells() {
		return () -> new ArrayGridIterator<Cell<E>>(){
			@Override
			public Cell<E> next() {
				if(hasNext())
					return new Cell<>(x, y, (E) data[x][y++]);
				throw new NoSuchElementException();
			}
		};
	}

    @Override
    public Iterator<E> iterator() {
		return new ArrayGridIterator<E>() {
			@Override
			public E next() {
				if(hasNext())
					return (E) data[x][y++];
				throw new NoSuchElementException();
			}
		};
	}

    private abstract class ArrayGridIterator<T> implements Iterator<T> {
		int x, y;

		@Override
		public boolean hasNext() {
			while (x < data.length) {
				Object[] inner = data[x];
				while (y < inner.length) {
					if (inner[y] != null)
                        return true;
					y++;
				}
				y = 0;
				x++;
			}
			return false;
		}
	}

	@Override
	public void compute(int x, int y, UnaryOperator<E> operator){
		checkInBounds(x, y);
		data[x][y] = operator.apply((E) data[x][y]);
	}

	public E[][] toArray() {
		Object[][] array = new Object[data.length][];
		for (int x = 0; x < data.length; x++) {
			array[x] = Arrays.copyOf(data[x], data[x].length);
		}
		return (E[][]) array;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("[");
		for(Cell<E> cell : cells()){
			sb.append(cell.toString()).append(", ");
		}
		return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
	}
}
