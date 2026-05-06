package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
import com.daninichu.util.DynamicQuadTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
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
@Warmup(iterations = 2, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeSearchBenchmark{

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    public int elementCount = 2000000
//            /2
            ;

    @Param({
            "0.01",
//            "0.20",
//            "1",
    })
    public double fraction;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 10000.0;
    private static final double ELEMENT_SIZE = 2.0;   // each element is 2x2 units
    private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final QuadTree<Object> quadTree = new QuadTree<>(worldBounds);
    private final DynamicQuadTree<Object> quadTree2 = new DynamicQuadTree<>(worldBounds);

    private final ArrayList<Object> result = new ArrayList<>(elementCount);
    private final Collection<DynamicQuadTree.Entry<Object>> result2 = new ArrayList<>(elementCount);

    private Rectangle2D searchArea;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Setup(Level.Trial)
    public void setUp() {
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
        }

        double side = WORLD * Math.sqrt(fraction);   // square centred in the world
        double offset = (WORLD - side) / 2.0;
        searchArea = new Rectangle2D.Double(offset, offset, side, side);
    }

    @Setup(Level.Invocation)
    public void clear(){
        result.clear();
        result2.clear();
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void quadTree(Blackhole bh) {
        quadTree.search(searchArea, result);
        bh.consume(result);
    }

    @Benchmark
    public void quadTree2(Blackhole bh) {
        quadTree2.search(searchArea, result2);
        bh.consume(result);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeSearchBenchmark.class);
    }
}
