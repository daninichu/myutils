package com.daninichu.util;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Grid points are in (x,y) coordinates.
 * @param <E>
 */
public interface Grid<E> extends Iterable<E> {
	/**
	 * @param x
	 * @param y
     * @return The value at the specified point, or null if there is none.
	 */
	E get(int x, int y);

	/**
     * @param p The point to retrieve a value from.
     * @return The value at the specified point, or null if there is none.
     * @throws NullPointerException if {@code p} is null.
     */
	E get(Point p);

    default E getOrDefault(int x, int y, E defaultValue){
		E e = get(x, y);
        return e == null ? defaultValue : e;
    }

    default E getOrDefault(Point p, E defaultValue){
        E e = get(p.x, p.y);
		return e == null ? defaultValue : e;
    }

    /**
	 * Overwrites the old value in the given point.
	 * This method does nothing if the {@code e} is null.
	 * @param x
	 * @param y
	 * @param e The new value.
	 */
	void set(int x, int y, E e);

	/**
	 * Overwrites the old value in the given point.
	 * This method does nothing if the {@code e} is null.
	 * @param p The point to set the new value.
	 * @param e The new value.
	 * @throws NullPointerException if {@code p} is null.
	 */
	void set(Point p, E e);

	/**
	 *
	 * @param grid
	 * @throws NullPointerException if {@code grid} is null.
	 */
	void setAll(Grid<? extends E> grid);

	/**
	 * Sets the value at the given point to null.
	 * @param x
	 * @param y
	 */
	void removePoint(int x, int y);

	/**
	 * Sets the value at the given point to null.
	 * @param p The point to remove the value from.
	 * @throws NullPointerException if {@code p} is null.
	 */
	void removePoint(Point p);

	void removeValue(E e);

	/**
	 * Checks if there exists a non-null value at the given point.
	 * @param x
	 * @param y
	 * @return True if there is a non-null value.
	 */
	boolean containsPoint(int x, int y);

	/**
	 * Checks if there exists a non-null value at the given point.
	 * @param p The point to check the existence of a value.
	 * @return True if there is a non-null value.
	 * @throws NullPointerException if {@code p} is null.
	 */
	boolean containsPoint(Point p);

	boolean containsValue(E e);

	/**
	 * Sets all values to null.
	 */
	void clear();

	/**
	 * @return An Iterable of all points in the grid with non-null values.
	 */
	Iterable<Point> points();

	/**
	 * @return An Iterable of all point and non-null value pairs in the grid.
	 */
	Iterable<Cell<E>> cells();

	final class Point{
		public final int x, y;

		public Point(int x, int y){
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj){
			return obj == this || obj instanceof Point p && x == p.x && y == p.y;
		}

		@Override
		public int hashCode(){
			return 65537 * x + y;
		}

		@Override
		public String toString(){
			return "(" + x + ',' + y + ')';
		}
	}

	final class Cell<E>{
		public final int x;
		public final int y;
		public final E value;

		public Cell(int x, int y, E value){
			this.x = x;
			this.y = y;
			this.value = value;
		}

		public Cell(Point p, E value){
			this.x = p.x;
			this.y = p.y;
			this.value = value;
		}

		@Override
		public boolean equals(Object obj){
			return obj == this || obj instanceof Cell<?> c && x == c.x && y == c.y && Objects.equals(value, c.value);
		}

		@Override
		public int hashCode(){
			return 65537 * (65537 * x + y) + Objects.hashCode(value);
		}

		@Override
		public String toString(){
			return "(" + x + ',' + y + ")=" + value;
		}
	}

	void compute(int x, int y, UnaryOperator<E> operator);
}