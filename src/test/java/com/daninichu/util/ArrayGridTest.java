package com.daninichu.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.daninichu.util.Grid.Cell;

public class ArrayGridTest {
	@Test
	void testDimensions(){
		ArrayGrid grid = new ArrayGrid(21, 30);
		assertEquals(21, grid.width());
		assertEquals(30, grid.height());

		assertThrows(IllegalArgumentException.class, () -> new ArrayGrid(0, 5000));
		assertThrows(IllegalArgumentException.class, () -> new ArrayGrid(-1, -5));
		assertThrows(IllegalArgumentException.class, () -> new ArrayGrid(10, 0));
	}

	@Test
	void testSet(){
		ArrayGrid<Integer> grid = new ArrayGrid<>(20, 30);

		List<Cell<Integer>> cells = new ArrayList<>();
		cells.add(new Cell<>(new Point(0, 0), 10));
		cells.add(new Cell<>(new Point(1, 1), -4));
		cells.add(new Cell<>(new Point(2, 5), 42));
		cells.add(new Cell<>(new Point(9, 19), 420));

		for (Cell<Integer> cell : cells) {
			grid.set(cell.point(), cell.value());
		}
		for (Cell<Integer> cell : grid.cells()) {
			assertTrue(cells.contains(cell));
		}
	}

	@Test
    void testFirstIteratorElement() {
        ArrayGrid<Integer> grid = new ArrayGrid<>(3, 3);
        grid.set(1, 1, 42);

        Iterator<Integer> it = grid.iterator();

        assertEquals(42, it.next());
    }

	@Test
	void testIteratorThrows() {
		ArrayGrid<Integer> grid = new ArrayGrid<>(3, 3);
		grid.set(0, 0, 1);
		grid.set(2, 2, 2);

		Iterator<Integer> it = grid.iterator();

		assertEquals(1, it.next());
		assertEquals(2, it.next());
		assertThrows(NoSuchElementException.class, it::next);
	}

	@Test
	void testIteratorHasNext() {
		ArrayGrid<Integer> grid = new ArrayGrid<>(2, 2);
		grid.set(0, 1, 10);

		Iterator<Integer> it = grid.iterator();

		assertTrue(it.hasNext());
		assertEquals(10, it.next());

		assertFalse(it.hasNext());
	}
}
