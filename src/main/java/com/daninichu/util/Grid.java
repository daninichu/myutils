package com.daninichu.util;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Grid points are in (x,y) coordinates.
 * @param <E>
 */
public interface Grid<E> extends Iterable<E> {
	record Cell<E>(Point point, E value){}
	
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
	 * Sets the value at the given point to null.
	 * @param x
	 * @param y
	 */
	void remove(int x, int y);

	/**
	 * Sets the value at the given point to null.
	 * @param p The point to remove the value from.
	 * @throws NullPointerException if {@code p} is null.
	 */
	void remove(Point p);

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

	static List<Point> getPoints(Rectangle2D r, double cellWidth, double  cellHeight){
		int x1 = (int) Math.floor((r.getX()) / cellWidth);
        int y1 = (int) Math.floor((r.getY()) / cellHeight);
        int x2 = (int) Math.ceil((r.getMaxX()) / cellWidth);
        int y2 = (int) Math.ceil((r.getMaxY()) / cellHeight);

		List<Point> points = new ArrayList<>();
        for(int x = x1; x < x2; x++){
            for(int y = y1; y < y2; y++){
                points.add(new Point(x, y));
            }
        }
		return points;
	}
}