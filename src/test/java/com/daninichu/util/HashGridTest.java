package com.daninichu.util;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class HashGridTest{
    @Test
    public void testImmutableKeys() {
        HashGrid<Integer> grid = new HashGrid<>();
        HashMap<Point, Integer> map = new HashMap<>();

        Point point = new Point(0, 0);
        grid.set(point, 1);
        map.put(point, 1);
        var es = map.entrySet();
        System.out.println(es);
        map.clear();
        System.out.println(es);

        assertTrue(grid.containsPoint(point));
        assertEquals(1, grid.get(point));

        point.x = -1;
        assertNull(grid.get(point));
        assertEquals(1, grid.get(0, 0));
        assertEquals(1, grid.get(new Point(0, 0)));
    }
}