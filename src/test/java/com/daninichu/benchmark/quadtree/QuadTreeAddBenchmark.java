package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
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

    public int elementCount = 100000
//            /2
            ;

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    private static final double WORLD = 10000.0;
    private static final double ELEMENT_SIZE = 1.0;   // each element is 2x2 units

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);
    private QuadTree<Integer> quadTree = new QuadTree<>(worldBounds);
    private QuadTree2<Integer> quadTree2 = new QuadTree2<>(worldBounds);
    private DynamicQuadTree<Integer> dynamicQuadTree = new DynamicQuadTree<>(worldBounds);
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
        quadTree.clear();
        quadTree2.clear();
        dynamicQuadTree.clear();
    }

    // -------------------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------------------

    @Benchmark
    public void quadTree(Blackhole bh) {
        for (int i = 0; i < elementCount; i++) {
            quadTree.add(i, linearStore.get(i));
        }
        bh.consume(quadTree);
    }

//    @Benchmark
    public void quadTree2(Blackhole bh) {
        for (int i = 0; i < elementCount; i++) {
            quadTree2.add(i, linearStore.get(i));
        }
        bh.consume(quadTree2);
    }

    @Benchmark
    public void dynamicQuadTree(Blackhole bh) {
        for (int i = 0; i < elementCount; i++) {
            dynamicQuadTree.add(i, linearStore.get(i));
        }
        bh.consume(dynamicQuadTree);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeAddBenchmark.class);
    }
}
