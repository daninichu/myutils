package com.daninichu.util;

import java.awt.Point;
import java.util.Objects;
import java.util.function.Function;

/**
 * A two-dimensional collection of values addressed by (x,y) coordinates,
 * where {@code x} is the column and {@code y} is the row.
 * @param <E> The type of values held in this grid.
 */
public interface Grid<E> extends Iterable<E>{
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
	 * Returns the value at the given point,
	 * or sets and returns the given value if the point is vacant.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param e The value to set if the point is vacant.
	 * @return The existing value at the given point, or {@code e} if the point was vacant.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if the point is vacant
	 * 		   and {@code e} is null and this grid does not permit null values.
	 */
	E getOrSet(int x, int y, E e);

	/**
	 * Returns the value at the given point,
	 * or sets and returns the given value if the point is vacant.
	 * @param p The point to get or set the value.
	 * @param e The value to set if the point is vacant.
	 * @return The existing value at the given point, or {@code e} if the point was vacant.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if {@code p} is null, or if the point is vacant
	 * 		   and {@code e} is null and this grid does not permit null values.
	 */
	E getOrSet(Point p, E e);

	/**
	 * Replaces the value at the given point with the result of applying {@code function} to it.
	 * The function receives null if there is no value at the point.
	 * If the function returns null, the value at the point is removed.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param function The function to apply to the current value.
	 * @return The new value at the given point, or null if the point is now vacant.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if {@code function} is null.
	 */
	E compute(int x, int y, Function<? super E, ? extends E> function);

	/**
	 * Replaces the value at the given point with the result of applying {@code function} to it.
	 * The function receives null if there is no value at the point.
	 * If the function returns null, the value at the point is removed.
	 * @param p The point to apply the function on.
	 * @param function The function to apply to the current value.
	 * @return The new value at the given point, or null if the point is now vacant.
	 * @throws IndexOutOfBoundsException if the point is out of bounds of this grid.
	 * @throws NullPointerException if {@code p} or {@code function} is null.
	 */
	E compute(Point p, Function<? super E, ? extends E> function);

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

	/**
	 * @param e The value to be removed.
	 * @return {@code true} if the value was in this grid.
	 */
	boolean removeValue(E e);

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
	 * @param grid The grid to set points from.
	 * @throws IndexOutOfBoundsException if any point in {@code grid} is out of bounds of this grid.
	 * @throws NullPointerException if {@code grid} is null,
	 * 		   or if any value from {@code grid} is null and this grid does not permit null values.
	 */
	void setAll(Grid<? extends E> grid);

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

	/**
	 * An immutable pairing of a point's (x,y) coordinates and the value at that point.
	 * <p>
	 * A {@code Cell} is a snapshot. It is not a view of the grid, so it does not
	 * reflect later changes to the grid, and modifying it is not possible.
	 * @param <E> The type of the value.
	 * @see Grid#cells()
	 */
	final class Cell<E>{
		/**
		 * The x coordinate of this cell.
		 */
		public final int x;

		/**
		 * The y coordinate of this cell.
		 */
		public final int y;

		/**
		 * The value at this cell's point.
		 */
		public final E value;

		/**
		 * Constructs a {@code Cell} with the given coordinates and value.
		 * This constructor does not check that the point is within any particular grid.
		 * @param x The x coordinate.
		 * @param y The y coordinate.
		 * @param value The value at the point.
		 */
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
			return 31 * (31 * x + y) + Objects.hashCode(value);
		}

		@Override
		public String toString(){
			return "(" + x + ',' + y + ")=" + value;
		}
	}
}
