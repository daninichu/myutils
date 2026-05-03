package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

/**
 * A matrix with a fixed size. Row and column coordinates cannot be negative.
 */
@SuppressWarnings("unchecked")
public class Matrix<E> {
    public record Cell<E>(int row, int col, E value) {
        @Override
        public String toString(){
            return "(" + row + "," + col + ")=" + value;
        }
    }
    private final Object[][] data;

    /**
     * Sets the dimensions of the matrix.
     *
     * @param rows
     * @param cols
     * @throws IllegalArgumentException if {@code rows} or {@code cols} are less than 1.
     */
    public Matrix(int rows, int cols) {
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException("rows and cols must be 1 or greater");
        }
        this.data = new Object[rows][cols];
    }

    public Matrix(Matrix<E> matrix) {
        this.data = new Object[matrix.rows()][];
        for (int row = 0; row < matrix.rows(); row++) {
            data[row] = Arrays.copyOf(matrix.data[row], matrix.cols());
        }
    }

    public int rows() {
        return data.length;
    }

    public int cols() {
        return data[0].length;
    }

    public boolean inBounds(int row, int col) {
        return 0 <= row && row < rows() && 0 <= col && col < cols();
    }

    private void checkInBounds(int row, int col) {
        if (!inBounds(row, col)) {
            throw new IndexOutOfBoundsException(
                    "(%d, %d) out of bounds for dimensions (%d rows, %d cols)"
                            .formatted(row, col, rows(), cols())
            );
        }
    }

    public void fill(E e) {
        for (Object[] inner : data) {
            Arrays.fill(inner, e);
        }
    }

    public E get(int row, int col) {
        checkInBounds(row, col);
        return (E) data[row][col];
    }

    public void set(int row, int col, E e) {
        checkInBounds(row, col);
        data[row][col] = e;
    }

    public void remove(int row, int col) {
        checkInBounds(row, col);
        data[row][col] = null;
    }

    public boolean containsPoint(int row, int col) {
        checkInBounds(row, col);
        return data[row][col] != null;
    }

    public boolean containsValue(E e) {
        if (e != null) {
            for (Object[] inner : data) {
                for (Object o : inner) {
                    if (o.equals(e)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clear() {
        for (Object[] inner : data) {
            Arrays.fill(inner, null);
        }
    }

    public Iterator<E> iterator() {
        return new MatrixIterator<E>() {
            public E next() {
                if (hasNext()) {
                    return (E) data[row][col++];
                }
                throw new NoSuchElementException();
            }
        };
    }

    private abstract class MatrixIterator<T> implements Iterator<T> {
        int row, col;

        public boolean hasNext() {
            while (row < data.length) {
                Object[] inner = data[row];
                while (col < inner.length) {
                    if (inner[col] != null) {
                        return true;
                    }
                    col++;
                }
                col = 0;
                row++;
            }
            return false;
        }
    }

    public void compute(int row, int col, UnaryOperator<E> operator) {
        checkInBounds(row, col);
        data[row][col] = operator.apply((E) data[row][col]);
    }

    public E[][] toArray() {
        Object[][] array = new Object[data.length][];
        for (int row = 0; row < data.length; row++) {
            array[row] = Arrays.copyOf(data[row], data[row].length);
        }
        return (E[][]) array;
    }

}