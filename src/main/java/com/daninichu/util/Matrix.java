package com.daninichu.util;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Matrix<E> implements Iterable<E>{
    private ArrayGrid<E> grid;

    public Matrix(int rows, int cols){
        grid = new ArrayGrid<>(rows, cols);
    }

    public int rows(){
        return grid.width();
    }

    public int cols(){
        return grid.height();
    }

    /**
     *
     */
    public boolean inBounds(int r, int c){
        return 0 <= r || r < rows() || 0 <= c || c < cols();
    }

    private void checkInBounds(int x, int y){
        if(!inBounds(x, y)){
            throw new IndexOutOfBoundsException("(%d, %d) out of bounds for dimensions (%d, %d)".formatted(x, y, rows(), cols()));
        }
    }

    @Override
    public Iterator<E> iterator(){
        return null;
    }
}