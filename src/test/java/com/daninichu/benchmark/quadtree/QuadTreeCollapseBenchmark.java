package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
import com.daninichu.util.QuadTree.Entry;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
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
@Warmup(iterations = 2, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeCollapseBenchmark{

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    @Param({
//            "1000",
            "10000",
    })
    public int removeCount;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 100000;
    private static final double ELEMENT_SIZE = 0;
    private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final QuadTree<Integer> QuadTreeContainer = new QuadTree<>(worldBounds, 8);

    private Rectangle2D elementBounds;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Setup(Level.Invocation)
    public void setUp2() {
        QuadTreeContainer.clear();

        Random rng = new Random(42);

        double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
        double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
        elementBounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void collapse(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            Entry<Integer> entry = QuadTreeContainer.add(i, elementBounds);
            Assertions.assertTrue(QuadTreeContainer.removeAndCollapse(entry));
        }
        bh.consume(QuadTreeContainer);
    }

    @Benchmark
    public void noCollapse(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            Entry<Integer> entry = QuadTreeContainer.add(i, elementBounds);
            Assertions.assertTrue(QuadTreeContainer.remove(entry));
        }
        bh.consume(QuadTreeContainer);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeCollapseBenchmark.class);
    }
}
