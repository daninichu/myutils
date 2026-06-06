package com.daninichu.util;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Grid points are in (x,y) coordinates.
 * @param <E>
 */
public interface Grid<E> extends Iterable<E> {
	/**
	 * @return The number of values in this grid.
	 */
	int size();

	/**
	 * @return {@code true} if this grid contains no values.
	 */
	boolean isEmpty();

	/**
	 * @param x The x coordinate.
	 * @param y The y coordinate.
     * @return The value at the given point, or null if there is none.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 */
	E get(int x, int y);

	/**
     * @param p The point to retrieve the value from.
     * @return The value at the given point, or null if there is none.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
     * @throws NullPointerException if {@code p} is null.
     */
	E get(Point p);

	/**
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param defaultValue The value to return if no value was found.
	 * @return The value at the given point, or the default value if there is none.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 */
	E getOrDefault(int x, int y, E defaultValue);

	/**
	 * @param p The point to retrieve the value from.
	 * @param defaultValue The value to return if no value was found.
	 * @return The value at the given point, or the default value if there is none.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if {@code p} is null.
	 */
	E getOrDefault(Point p, E defaultValue);

    /**
	 * Overwrites the value at the given point.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param e The new value.
	 * @return The value previously at the given point.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if {@code e} is null and this grid does not permit null values.
	 */
	E set(int x, int y, E e);

	/**
	 * Overwrites the value at the given point.
	 * @param p The point to set the new value.
	 * @param e The new value.
	 * @return The value previously at the given point.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if {@code p} is null,
	 * 		   or if {@code e} is null and this grid does not permit null values.
	 */
	E set(Point p, E e);

	/**
	 *
	 * @param grid The grid to set points from.
	 * @throws IndexOutOfBoundsException if any point in {@code grid} is out of bounds of this grid.
	 * @throws NullPointerException if {@code grid} is null,
	 * 		   or if any value from {@code grid} is null and this grid does not permit null values.
	 */
	void setAll(Grid<? extends E> grid);

	/**
	 * Removes the value at the given point.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @return The value previously at the given point.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 */
	E removePoint(int x, int y);

	/**
	 * Removes the value at the given point.
	 * @param p The point to remove the value from.
	 * @return The value previously at the given point.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
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
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @return {@code true} if there is a value.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 */
	boolean containsPoint(int x, int y);

	/**
	 * Checks if there exists a value at the given point.
	 * @param p The point to check the existence of a value.
	 * @return {@code true} if there is a value.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
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