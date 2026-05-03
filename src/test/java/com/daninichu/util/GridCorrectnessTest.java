package com.daninichu.util;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
public class GridCorrectnessTest{
    int width = 10;
    int height = 20;
    List<Grid<Integer>> gridList = new ArrayList<>();

    {
        gridList.add(new ArrayGrid<>(width, height));
        gridList.add(new HashGrid<>());
    }

    private void setAll(int x, int y, Integer value) {
        for(Grid<Integer> grid : gridList){
            grid.set(x, y, value);
        }
    }

    @Test
    public void testSet() {
        for(var grid : gridList){
            assertNull(grid.get(0, 0));
        }
        setAll(0, 0, 99);
        gridList.forEach(grid -> assertEquals(99, grid.get(0, 0)));

        setAll(9, 0, 11);
        gridList.forEach(grid -> assertEquals(11, grid.get(9, 0)));

        setAll(0, 0, 42);
        gridList.forEach(grid -> assertEquals(42, grid.get(0, 0)));
    }

    @Test
    void testForEach(){
        List<Integer> values = new ArrayList<>();

        int i = 0;
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                setAll(x, y, i);
                values.add(i);
                i++;
            }
        }

        for(Grid<Integer> grid : gridList){
            i = 0;
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    assertEquals(i, grid.get(x, y));
                    i++;
                }
            }
        }
    }
}