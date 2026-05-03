package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing QuadTree spatial search against brute-force linear scan.
 *
 * Three search-area sizes are tested against three element counts to show how the
 * performance gap between the two strategies varies with load and query breadth:
 *
 *   - SMALL  search area  →  QuadTree wins most here; few quadrants are visited.
 *   - MEDIUM search area  →  Moderate advantage.
 *   - LARGE  search area  →  Gap narrows; copyElements() bulk path helps QuadTree.
 *
 * Run with:
 *   mvn clean package && java -jar target/benchmarks.jar QuadTreeBenchmark
 *
 * Or from an IDE: call main() directly (uses a quick 1-fork / 1-iteration setup).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class QuadTreeBenchmark {

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    /** Total number of elements inserted into both structures. */
    @Param({"1000000"})
    public int elementCount;

    /**
     * Fraction of the world that the search rectangle covers.
     * SMALL = 5 %, MEDIUM = 25 %, LARGE = 60 %.
     */
    @Param({"0.05", "0.60"})
    public double fraction;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 1000.0;
    private static final double ELEMENT_SIZE = 2.0;   // each element is 2x2 units

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private QuadTree<Integer> quadTree;
    private List<Rectangle2D> linearStore;   // brute-force "index"
    private List<Integer>     linearValues;  // parallel list of elements

    private Rectangle2D searchArea;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Setup(Level.Trial)
    public void setUp() {
        Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);
        quadTree     = new QuadTree<>(worldBounds);
        linearStore  = new ArrayList<>(elementCount);
        linearValues = new ArrayList<>(elementCount);

        Random rng = new Random(42);   // fixed seed for reproducible layout

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            quadTree.add(i, bounds);
            linearStore.add(bounds);
            linearValues.add(i);
        }

        double side = WORLD * Math.sqrt(fraction);   // square centred in the world
        double offset = (WORLD - side) / 2.0;
        searchArea = new Rectangle2D.Double(offset, offset, side, side);
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void quadTreeSearch(Blackhole bh) {
        bh.consume(quadTree.search(searchArea));
    }

    @Benchmark
    public void bruteForceSearch(Blackhole bh) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < linearStore.size(); i++) {
            if (linearStore.get(i).intersects(searchArea)) {
                result.add(linearValues.get(i));
            }
        }
        bh.consume(result);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeBenchmark.class);
    }
}
