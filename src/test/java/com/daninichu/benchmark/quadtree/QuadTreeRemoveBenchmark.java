package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree2;
import com.daninichu.util.QuadTree;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
public class QuadTreeRemoveBenchmark{

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    @Param({
//            "100000",
            "200000",
    })
    public int elementCount;
    @Param({
//            "1000",
            "20000",
    })
    public int removeCount;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 1000000;
    private static final double ELEMENT_SIZE = 0;
    private static final int MAX_DEPTH = 8;
    private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);
    int m;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final QuadTree2<Integer> QuadTree2 = new QuadTree2<>(worldBounds, MAX_DEPTH);
    private final QuadTree<Integer> QuadTree = new QuadTree<>(worldBounds, MAX_DEPTH);

    private final ArrayList<QuadTree2.Entry<Integer>> toRemove2 = new ArrayList<>(elementCount);
    private final ArrayList<QuadTree.Entry<Integer>> toRemoveContainer = new ArrayList<>(elementCount);

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------


//    @Setup(Level.Trial)
//    public void setUp() {
//        setUp2();
//        System.out.println(m);
//    }

    @Setup(Level.Invocation)
    public void setUp2() {
        QuadTree2.clear();
        QuadTree.clear();
        toRemove2.clear();
        toRemoveContainer.clear();


        Random rng = new Random(42);

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            QuadTree2.Entry<Integer> entry2 = QuadTree2.add(i, bounds);
            toRemove2.add(entry2);

            QuadTree.Entry<Integer> entry3 = QuadTree.add(i, bounds);
            toRemoveContainer.add(entry3);
//            m = Math.max(entry3.quadrant.entries.size(), m);
        }
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void QuadTree2(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            QuadTree2.Entry<Integer> entry = toRemove2.get(i);
            Assertions.assertTrue(QuadTree2.removeAndCollapse(entry));
        }
        bh.consume(QuadTree2);
    }

    @Benchmark
    public void QuadTree(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            QuadTree.Entry<Integer> entry = toRemoveContainer.get(i);
            Assertions.assertTrue(QuadTree.removeAndCollapse(entry));
        }
        bh.consume(QuadTree);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeRemoveBenchmark.class);
    }
}
