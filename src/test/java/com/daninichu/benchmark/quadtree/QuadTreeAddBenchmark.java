package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.IterativeQuadTree;
import com.daninichu.util.QuadTree;
import com.daninichu.util.DynamicQuadTree;
import com.daninichu.util.QuadTree2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.AverageTime,
//        Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeAddBenchmark{

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    public int elementCount = 1000000
//            /2
            ;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 10000.0;
    private static final double ELEMENT_SIZE = 0;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);
    private QuadTree<Integer> QuadTree = new QuadTree<>(worldBounds);
    private IterativeQuadTree<Integer> IterativeQuadTree = new IterativeQuadTree<>(worldBounds);
    private List<Rectangle2D> linearStore;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Setup(Level.Trial)
    public void setUp() {
        linearStore  = new ArrayList<>(elementCount);

        Random rng = new Random(42);   // fixed seed for reproducible layout

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            linearStore.add(bounds);
        }
    }

    @Setup(Level.Invocation)
    public void clear(){
        QuadTree = new QuadTree<>(worldBounds);
        IterativeQuadTree = new IterativeQuadTree<>(worldBounds);
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void QuadTree(Blackhole bh) {
        for (int i = 0; i < elementCount; i++) {
            QuadTree.add(i, linearStore.get(i));
        }
        bh.consume(QuadTree);
    }

    @Benchmark
    public void IterativeQuadTree(Blackhole bh) {
        for (int i = 0; i < elementCount; i++) {
            IterativeQuadTree.add(i, linearStore.get(i));
        }
        bh.consume(IterativeQuadTree);
    }
//    @Benchmark
    public void QuadTre2(Blackhole bh) {
        for (int i = 0; i < elementCount; i++) {
            IterativeQuadTree.add(i, linearStore.get(i));
        }
        bh.consume(IterativeQuadTree);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeAddBenchmark.class);
    }
}
