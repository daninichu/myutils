package com.daninichu.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuadTreeTest {

    private static final Rectangle2D ROOT_BOUNDS = new Rectangle2D.Double(0, 0, 100, 100);

    private QuadTree<String> tree;

    @BeforeEach
    void setUp() {
        tree = new QuadTree<>(ROOT_BOUNDS);
    }

    /** Small 1×1 rectangle with its top-left corner at (x, y). */
    private Rectangle2D point(double x, double y) {
        return new Rectangle2D.Double(x, y, 1, 1);
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Nested
    class InitialState {

        @Test
        void sizeIsZeroAfterCreation() {
            assertEquals(0, tree.size());
        }

        @Test
        void searchOnEmptyTreeReturnsEmptyList() {
            assertTrue(tree.search(ROOT_BOUNDS).isEmpty());
        }
    }

    // =========================================================================
    // add() + size()
    // =========================================================================

    @Nested
    class AddAndSize {

        @Test
        void addingSingleElementIncreasesSize() {
            tree.add("A", point(10, 10));
            assertEquals(1, tree.size());
        }

        @Test
        void addingMultipleElementsTracksSize() {
            tree.add("A", point(10, 10));
            tree.add("B", point(60, 10));
            tree.add("C", point(10, 60));
            tree.add("D", point(60, 60));
            assertEquals(4, tree.size());
        }

        @Test
        void elementSpanningQuadrantBoundaryStoredAtParent() {
            // Straddles the centre line — doesn't fit in any single child quadrant
            Rectangle2D straddling = new Rectangle2D.Double(40, 40, 20, 20);
            tree.add("Straddler", straddling);
            assertEquals(1, tree.size());
        }

        @Test
        void elementFittingInsideChildQuadrantDelegatedToChild() {
            tree.add("TL", point(10, 10));
            assertEquals(1, tree.size());
        }

        @Test
        void duplicateElementsAreAllStored() {
            tree.add("Dup", point(10, 10));
            tree.add("Dup", point(10, 10));
            assertEquals(2, tree.size());
        }
    }

    // =========================================================================
    // search()
    // =========================================================================

    @Nested
    class Search {

        @Test
        void searchFindsIntersectingElement() {
            tree.add("A", point(10, 10));
            ArrayList<String> result = tree.search(new Rectangle2D.Double(9, 9, 5, 5));
            assertTrue(result.contains("A"));
        }

        @Test
        void searchDoesNotReturnNonIntersectingElement() {
            tree.add("A", point(10, 10));
            ArrayList<String> result = tree.search(new Rectangle2D.Double(80, 80, 5, 5));
            assertFalse(result.contains("A"));
        }

        @Test
        void searchEntireAreaReturnsAllElements() {
            tree.add("A", point(10, 10));
            tree.add("B", point(60, 10));
            tree.add("C", point(10, 60));
            tree.add("D", point(60, 60));

            ArrayList<String> result = tree.search(ROOT_BOUNDS);
            assertEquals(4, result.size());
            assertTrue(result.containsAll(List.of("A", "B", "C", "D")));
        }

        @Test
        void searchPartialAreaFiltersCorrectly() {
            tree.add("InArea",    point(10, 10));
            tree.add("OutOfArea", point(70, 70));

            ArrayList<String> result = tree.search(new Rectangle2D.Double(0, 0, 50, 50));
            assertTrue(result.contains("InArea"));
            assertFalse(result.contains("OutOfArea"));
        }

        @Test
        void searchWithCollectionOverloadAppendsToExistingList() {
            tree.add("A", point(10, 10));
            ArrayList<String> existing = new ArrayList<>();
            existing.add("Pre-existing");

            tree.search(ROOT_BOUNDS, existing);

            assertTrue(existing.contains("Pre-existing"));
            assertTrue(existing.contains("A"));
        }

        @Test
        void searchAreaContainingWholeChildQuadrantCopiesAllChildElements() {
            tree.add("TL", point(10, 10));
            tree.add("TR", point(60, 10));
            tree.add("BL", point(10, 60));
            tree.add("BR", point(60, 60));

            ArrayList<String> result = tree.search(ROOT_BOUNDS);
            assertEquals(4, result.size());
        }

        @Test
        void searchZeroSizeAreaReturnsEmpty() {
            tree.add("A", point(10, 10));
            ArrayList<String> result = tree.search(new Rectangle2D.Double(90, 90, 0, 0));
            assertTrue(result.isEmpty());
        }

        @Test
        void elementOnQuadrantBorderIsFound() {
            // Straddles the centre — stored at the root, must still be found
            tree.add("Centre", new Rectangle2D.Double(49, 49, 2, 2));
            assertTrue(tree.search(ROOT_BOUNDS).contains("Centre"));
        }

        @Test
        void nullElementIsStoredAndReturned() {
            tree.add(null, point(10, 10));
            ArrayList<String> result = tree.search(ROOT_BOUNDS);
            assertEquals(1, result.size());
            assertNull(result.get(0));
        }
    }

    // =========================================================================
    // clear()
    // =========================================================================

    @Nested
    class Clear {

        @Test
        void clearMakesSizeZero() {
            tree.add("A", point(10, 10));
            tree.add("B", point(60, 60));
            tree.clear();
            assertEquals(0, tree.size());
        }

        @Test
        void clearMakesSearchReturnEmpty() {
            tree.add("A", point(10, 10));
            tree.clear();
            assertTrue(tree.search(ROOT_BOUNDS).isEmpty());
        }

        @Test
        void addingAfterClearWorks() {
            tree.add("A", point(10, 10));
            tree.clear();
            tree.add("B", point(20, 20));
            assertEquals(1, tree.size());
            assertTrue(tree.search(ROOT_BOUNDS).contains("B"));
        }
    }

    // =========================================================================
    // resize()
    // =========================================================================

    @Nested
    class Resize {

        @Test
        void resizeClearsExistingElements() {
            tree.add("A", point(10, 10));
            tree.resize(ROOT_BOUNDS);
            assertEquals(0, tree.size());
        }

        @Test
        void resizeAllowsAddingElementsInNewBounds() {
            Rectangle2D newBounds = new Rectangle2D.Double(0, 0, 200, 200);
            tree.resize(newBounds);
            tree.add("Far", point(150, 150));
            assertEquals(1, tree.size());
        }
    }

    // =========================================================================
    // MAX_DEPTH enforcement
    // =========================================================================

    @Nested
    class MaxDepth {

        @Test
        void elementsAtMaxDepthAreStoredAndRetrievable() {
            // Many tiny elements in the same region force subdivision down to MAX_DEPTH,
            // after which elements must be stored in the leaf rather than subdivided further.
            for (int i = 0; i < 25; i++) {
                tree.add("E" + i, point(1 + i * 0.001, 1 + i * 0.001));
            }
            ArrayList<String> result = tree.search(new Rectangle2D.Double(0, 0, 10, 10));
            assertEquals(25, result.size());
        }
    }
}
