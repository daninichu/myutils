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

	E getOrDefault(int x, int y, E defaultValue);

	E getOrDefault(Point p, E defaultValue);

    /**
	 * Overwrites the old value in the given point.
	 * @param e The new value.
	 * @throws NullPointerException if {@code e} is null and this grid does not permit null values.
	 */
	E set(int x, int y, E e);

	/**
	 * Overwrites the old value in the given point.
	 * @param p The point to set the new value.
	 * @param e The new value.
	 * @throws NullPointerException if {@code p} is null,
	 * 		   or if {@code e} is null and this grid does not permit null values.
	 */
	E set(Point p, E e);

	/**
	 *
	 * @param grid
	 * @throws NullPointerException if {@code grid} is null,
	 * 		   or if any value from {@code grid} is null and this grid does not permit null values.
	 */
	void setAll(Grid<? extends E> grid);

	/**
	 * Removes the value at the given point.
	 * @param x
	 * @param y
	 * @return The value previously at the given point.
	 */
	E removePoint(int x, int y);

	/**
	 * Removes the value at the given point.
	 * @param p The point to remove the value from.
	 * @return The value previously at the given point.
	 * @throws NullPointerException if {@code p} is null.
	 */
	E removePoint(Point p);

	/**
	 * @param e The value to be removed.
	 * @return {@code true} if the value was in this grid.
	 */
	boolean removeValue(E e);

	/**
	 * Checks if there exists a value at the given point.
	 * @param x
	 * @param y
	 * @return {@code true} if there is a value.
	 */
	boolean containsPoint(int x, int y);

	/**
	 * Checks if there exists a value at the given point.
	 * @param p The point to check the existence of a value.
	 * @return {@code true} if there is a value.
	 * @throws NullPointerException if {@code p} is null.
	 */
	boolean containsPoint(Point p);

	/***
	 * @param e The value whose presence in this grid is to be tested.
	 * @return {@code true} the given value is in this grid.
	 */
	boolean containsValue(E e);

	/**
	 * @param e The value to search for.
	 * @return One of the points that has the given value, or null if the value is not in this grid.
	 */
	Point pointOf(E e);

	/**
	 * Removes all values from this grid.
	 */
	void clear();

	/**
	 * @return An Iterable of all points in the grid with values.
	 */
	Iterable<Point> points();

	/**
	 * @return An Iterable of all point and value pairs in the grid.
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