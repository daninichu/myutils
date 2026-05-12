package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.DynamicQuadTree;
import com.daninichu.util.DynamicQuadTree2;
import com.daninichu.util.QuadTreeContainer;
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
            "10000",
//            "20000",
    })
    public int elementCount;
    @Param({
            "1000",
//            "10000",
    })
    public int removeCount;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 10000.0;
    private static final double ELEMENT_SIZE = 2.0;   // each element is 2x2 units
    private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final DynamicQuadTree<Integer> DynamicQuadTree = new DynamicQuadTree<>(worldBounds);
    private final DynamicQuadTree2<Integer> DynamicQuadTree2 = new DynamicQuadTree2<>(worldBounds);
    private final QuadTreeContainer<Integer> QuadTreeContainer = new QuadTreeContainer<>(worldBounds);

    private final ArrayList<DynamicQuadTree.Entry<Integer>> toRemove = new ArrayList<>(elementCount);
    private final ArrayList<DynamicQuadTree2.Entry<Integer>> toRemove2 = new ArrayList<>(elementCount);
    private final ArrayList<QuadTreeContainer.Entry<Integer>> toRemoveContainer = new ArrayList<>(elementCount);

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Setup(Level.Invocation)
    public void setUp2() {
        DynamicQuadTree.clear();
        DynamicQuadTree2.clear();
        QuadTreeContainer.clear();
        toRemove.clear();
        toRemove2.clear();
        toRemoveContainer.clear();


        Random rng = new Random(42);

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            DynamicQuadTree.Entry<Integer> entry = DynamicQuadTree.add(i, bounds);
            toRemove.add(entry);

            DynamicQuadTree2.Entry<Integer> entry2 = DynamicQuadTree2.add(i, bounds);
            toRemove2.add(entry2);

            QuadTreeContainer.Entry<Integer> entry3 = QuadTreeContainer.add(i, bounds);
            toRemoveContainer.add(entry3);
        }
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

//    @Benchmark
    public void DynamicQuadTree(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            DynamicQuadTree.Entry<Integer> entry = toRemove.get(i);
            Assertions.assertTrue(DynamicQuadTree.remove(entry.element));
        }
        bh.consume(DynamicQuadTree);
    }

    @Benchmark
    public void DynamicQuadTree2(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            DynamicQuadTree2.Entry<Integer> entry = toRemove2.get(i);
            Assertions.assertTrue(DynamicQuadTree2.removeAndCollapse(entry));
        }
        bh.consume(DynamicQuadTree2);
    }

    @Benchmark
    public void QuadTreeContainer(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            QuadTreeContainer.Entry<Integer> entry = toRemoveContainer.get(i);
            Assertions.assertTrue(QuadTreeContainer.removeAndCollapse(entry));
        }
        bh.consume(QuadTreeContainer);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeRemoveBenchmark.class);
    }
}
