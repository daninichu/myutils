package com.daninichu.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.badlogic.gdx.math.Vector2;
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

	@Test
	void hasNextIsIdempotent() {
		ArrayGrid<String> grid = new ArrayGrid<>(3, 3);

		grid.set(0, 0, "a");
		Iterator<String> it = grid.iterator();
		assertTrue(it.hasNext());
		assertTrue(it.hasNext()); // second call must not advance
		assertEquals("a", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void forEachLoopReturnsAllElements() {
		ArrayGrid<String> grid = new ArrayGrid<>(3, 3);

		// for-each calls hasNext() then next(), which internally called hasNext() again — the bug
		grid.set(0, 0, "a");
		grid.set(0, 1, "b");
		grid.set(1, 0, "c");

		List<String> result = new ArrayList<>();
		for (String s : grid) result.add(s);

		assertEquals(3, result.size()); // would be 2 with the bug
		assertTrue(result.containsAll(List.of("a", "b", "c")));
	}

	@Test
	void nextWithoutHasNextWorks() {
		ArrayGrid<String> grid = new ArrayGrid<>(3, 3);

		grid.set(0, 0, "a");
		grid.set(0, 1, "b");
		Iterator<String> it = grid.iterator();
		assertEquals("a", it.next()); // no hasNext() call first
		assertEquals("b", it.next());
		assertThrows(NoSuchElementException.class, it::next);
	}

	@Test
	void nullsAreSkipped() {
		ArrayGrid<String> grid = new ArrayGrid<>(3, 3);

		grid.set(0, 0, "a");
		// (0,1) left null
		grid.set(0, 2, "b");

		List<String> result = new ArrayList<>();
		grid.forEach(result::add);

		assertEquals(List.of("a", "b"), result);
	}

	@Test
	void emptyGridHasNoElements() {
		ArrayGrid<String> grid = new ArrayGrid<>(3, 3);

		Iterator<String> it = grid.iterator();
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}

	@Test
	void pointsMatchValues() {
		ArrayGrid<String> grid = new ArrayGrid<>(3, 3);

		grid.set(0, 1, "a");
		grid.set(1, 2, "b");

		Iterator<Grid.Point> pts = grid.points().iterator();
		Iterator<String> vals = grid.iterator();

		while (pts.hasNext()) {
			Grid.Point p = pts.next();
			String v = vals.next();
			assertEquals(v, grid.get(p.x, p.y));
		}
		assertFalse(vals.hasNext());
	}
}
