package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
import com.daninichu.util.QuadTree2;
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
@BenchmarkMode({
        Mode.AverageTime,
//        Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class QuadTreeBenchmark {

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    public int elementCount = 1000000;

    /**
     * Fraction of the world that the search rectangle covers.
     * SMALL = 5 %, MEDIUM = 25 %, LARGE = 60 %.
     */
    @Param({
//            "0.01",
            "0.25",
//            "1",
    })
    public double fraction;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 1000.0;
    private static final double ELEMENT_SIZE = 2.0;   // each element is 2x2 units

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private QuadTree<Object> quadTree, quadTreeHeavy;
    private QuadTree2<Object> quadTree2;
    private List<Rectangle2D> linearStore;   // brute-force "index"
    private List<Object>     linearValues;  // parallel list of elements

    private Rectangle2D searchArea;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Setup(Level.Trial)
    public void setUp() {
        Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);
        quadTree     = new QuadTree<>(worldBounds);
//        quadTreeHeavy = new QuadTree<>(worldBounds);
        quadTree2 = new QuadTree2<>(worldBounds);
        linearStore  = new ArrayList<>(elementCount);
        linearValues = new ArrayList<>(elementCount);

        Random rng = new Random(42);   // fixed seed for reproducible layout

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            Object e;
//            e = "p".repeat(790);
            e = i;
            quadTree.add(e, bounds);
            quadTree2.add(e, bounds);
            linearStore.add(bounds);
            linearValues.add(e);
        }

        double side = WORLD * Math.sqrt(fraction);   // square centred in the world
        double offset = (WORLD - side) / 2.0;
        searchArea = new Rectangle2D.Double(offset, offset, side, side);
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void quadTree(Blackhole bh) {
        bh.consume(quadTree.search(searchArea));
    }
//    @Benchmark
    public void quadTreeHeavySearch(Blackhole bh) {
        bh.consume(quadTreeHeavy.search(searchArea));
    }

    @Benchmark
    public void quadTree2(Blackhole bh) {
        bh.consume(quadTree2.search(searchArea));
    }

//    @Benchmark
    public void bruteForceSearch(Blackhole bh) {
        ArrayList<Object> result = new ArrayList<>();
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
