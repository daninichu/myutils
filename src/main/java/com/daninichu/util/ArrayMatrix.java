package com.daninichu.util;

import java.awt.Point;
import java.util.Iterator;
import java.util.function.UnaryOperator;

/**
 * A matrix with a fixed size. Row and column coordinates cannot be negative.
 * Backed by an {@link ArrayGrid}, translating (row, col) to (x=col, y=row).
 */
public class ArrayMatrix<E>  {
    private final ArrayGrid<E> grid;

    /**
     * @param rows
     * @param cols
     * @throws IllegalArgumentException if {@code rows} or {@code cols} are less than 1.
     */
    public ArrayMatrix(int rows, int cols) {
        this.grid = new ArrayGrid<>(cols, rows);
    }

    public int rows() {
        return grid.height();
    }

    public int cols() {
        return grid.width();
    }

    public boolean inBounds(int row, int col) {
        return grid.inBounds(col, row);
    }

    public void fill(E e) {
        grid.fill(e);
    }

    public E get(int row, int col) {
        return grid.get(col, row);
    }

    public void set(int row, int col, E e) {
        grid.set(col, row, e);
    }

}