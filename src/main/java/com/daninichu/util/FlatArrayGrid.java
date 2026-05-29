package com.daninichu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A grid with a fixed size. (x,y) coordinates cannot be negative.
 * <p>
 * Backed by a single flat {@code Object[]} in row-major (y-major) order.
 * Index mapping: {@code index = y * width + x}.
 * Compared to {@link ArrayGrid}, this avoids the overhead of pointer-chasing
 * through an array-of-arrays, yielding better cache locality.
 */
@SuppressWarnings("unchecked")
public class FlatArrayGrid<E> implements Grid<E> {
    private final Object[] data;
    private final int width;
    private final int height;

    /**
     * Creates a grid with the given dimensions. All cells start null.
     *
     * @param width
     * @param height
     * @throws IllegalArgumentException if {@code width} or {@code height} are less than 1.
     */
    public FlatArrayGrid(int width, int height) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("width and height must be 1 or greater");
        this.width = width;
        this.height = height;
        this.data = new Object[width * height];
    }

    public FlatArrayGrid(FlatArrayGrid<? extends E> grid) {
        this.width = grid.width;
        this.height = grid.height;
        this.data = Arrays.copyOf(grid.data, grid.data.length);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private int index(int x, int y) {
        return y * width + x;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean inBounds(Point p) {
        return p.x >= 0 && p.x < width && p.y >= 0 && p.y < height;
    }

    private void checkInBounds(int x, int y) {
        if (!inBounds(x, y))
            throw new IndexOutOfBoundsException(
                    "Point (%d,%d) out of bounds for dimensions (%d,%d)".formatted(x, y, width, height)
            );
    }

    public void fill(E e) {
        Arrays.fill(data, e);
    }

    // -------------------------------------------------------------------------
    // Grid<E> implementation
    // -------------------------------------------------------------------------

    /**
     * @throws IndexOutOfBoundsException If the point is out of bounds.
     */
    @Override
    public E get(int x, int y) {
        checkInBounds(x, y);
        return (E) data[index(x, y)];
    }

    /**
     * @throws NullPointerException      {@inheritDoc}
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
        data[index(x, y)] = Objects.requireNonNull(e);
    }

    /**
     * @throws NullPointerException      {@inheritDoc}
     * @throws IndexOutOfBoundsException If the point is out of bounds.
     */
    @Override
    public void set(Point p, E e) {
        set(p.x, p.y, e);
    }

    /**
     * @throws NullPointerException      {@inheritDoc}
     * @throws IndexOutOfBoundsException If any point in {@code grid} is out of bounds.
     */
    @Override
    public void setAll(Grid<? extends E> grid) {
        for (Cell<? extends E> cell : grid.cells())
            set(cell.x, cell.y, cell.value);
    }

    /**
     * @throws IndexOutOfBoundsException If the point is out of bounds.
     */
    @Override
    public boolean removePoint(int x, int y) {
        checkInBounds(x, y);
        int index = index(x, y);
        Object o = data[index];
        data[index] = null;
        return o != null;
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws IndexOutOfBoundsException If the point is out of bounds.
     */
    @Override
    public boolean removePoint(Point p) {
        return removePoint(p.x, p.y);
    }

    @Override
    public boolean removeValue(E e) {
        if (e != null) {
            for (int i = 0; i < data.length; i++) {
                if (e.equals(data[i])) {
                    data[i] = null;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @throws IndexOutOfBoundsException If the point is out of bounds.
     */
    @Override
    public boolean containsPoint(int x, int y) {
        checkInBounds(x, y);
        return data[index(x, y)] != null;
    }

    /**
     * @throws NullPointerException      {@inheritDoc}
     * @throws IndexOutOfBoundsException If the point is out of bounds.
     */
    @Override
    public boolean containsPoint(Point p) {
        return containsPoint(p.x, p.y);
    }

    @Override
    public boolean containsValue(E e) {
        if (e != null)
            for (Object o : data)
                if (e.equals(o))
                    return true;
        return false;
    }

    @Override
    public void clear() {
        Arrays.fill(data, null);
    }

    // -------------------------------------------------------------------------
    // Iterables
    // -------------------------------------------------------------------------

    @Override
    public Iterable<Point> points() {
        return () -> new FlatGridIterator<Point>() {
            @Override
            public Point next() {
                if (hasNext()) {
                    lastIdx = idx++;
                    return new Point(lastIdx % width, lastIdx / width);
                }
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells() {
        return () -> new FlatGridIterator<Cell<E>>() {
            @Override
            public Cell<E> next() {
                if (hasNext()) {
                    lastIdx = idx++;
                    return new Cell<>(lastIdx % width, lastIdx / width, (E) data[lastIdx]);
                }
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public Iterator<E> iterator() {
        return new FlatGridIterator<E>() {
            @Override
            public E next() {
                if (hasNext()) {
                    lastIdx = idx++;
                    return (E) data[lastIdx];
                }
                throw new NoSuchElementException();
            }
        };
    }

    private abstract class FlatGridIterator<T> implements Iterator<T> {
        int idx = 0;
        int lastIdx = -1;

        @Override
        public boolean hasNext() {
            while (idx < data.length) {
                if (data[idx] != null)
                    return true;
                idx++;
            }
            return false;
        }

        @Override
        public void remove() {
            if (lastIdx == -1)
                throw new IllegalStateException();
            data[lastIdx] = null;
            lastIdx = -1;
        }
    }

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator) {
        checkInBounds(x, y);
        int i = index(x, y);
        data[i] = operator.apply((E) data[i]);
    }

    // -------------------------------------------------------------------------
    // Extras
    // -------------------------------------------------------------------------

    /**
     * Returns a copy of the backing flat array.
     * Index mapping: {@code index = y * width + x}.
     */
    public Object[] toArray() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof FlatArrayGrid<?> other
                && width == other.width
                && height == other.height
                && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(data);
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Cell<E> cell : cells())
            sb.append(cell).append(", ");
        if (sb.length() > 1)
            sb.delete(sb.length() - 2, sb.length());
        return sb.append(']').toString();
    }
}
