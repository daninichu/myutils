package com.daninichu.benchmark.grid;

import com.daninichu.util.Grid;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public final class GridUtils {

    static List<Grid.Point> getPoints(Rectangle2D r, double cellWidth, double  cellHeight){
        int x1 = (int) Math.floor((r.getX()) / cellWidth);
        int y1 = (int) Math.floor((r.getY()) / cellHeight);
        int x2 = (int) Math.ceil((r.getMaxX()) / cellWidth);
        int y2 = (int) Math.ceil((r.getMaxY()) / cellHeight);

        List<Grid.Point> points = new ArrayList<>();
        for(int x = x1; x < x2; x++){
            for(int y = y1; y < y2; y++){
                points.add(new Grid.Point(x, y));
            }
        }
        return points;
    }

    public static void setAll(Grid<Integer> grid, Iterable<Grid.Cell<Integer>> cells) {
        for (var cell : cells) {
            grid.set(cell.x, cell.y, cell.value);
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