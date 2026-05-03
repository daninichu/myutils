package com.daninichu.benchmark.grid;

import com.daninichu.util.Grid;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public final class GridUtils {
    public static void setAll(Grid<Integer> grid, Iterable<Grid.Cell<Integer>> cells) {
        for (var cell : cells) {
            grid.set(cell.x(), cell.y(), cell.value());
        }
    }

    public static List<Grid.Cell<Integer>> cells(int width, int height) {
        List<Grid.Cell<Integer>> cells = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells.add(new Grid.Cell<>(x, y, x + y));
            }
        }
        return cells;
    }

    public static void fill(Grid<Integer> grid, int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid.set(x, y, x + y);
            }
        }
    }
}