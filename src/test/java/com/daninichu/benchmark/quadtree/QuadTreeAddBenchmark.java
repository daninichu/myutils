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

@BenchmarkMode({
//        Mode.AverageTime,
        Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeAddBenchmark{
    public int n = 1000000;

    private static final int WORLD = 100000;
    private static final double ELEMENT_SIZE = 0;

    private Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);
    private QuadTree<Integer> QuadTree = new QuadTree<>(worldBounds, 16);
    private List<Rectangle2D> linearStore;

    @Setup(Level.Trial)
    public void setUp() {
        linearStore  = new ArrayList<>(n);

        Random rng = new Random(42);

        for (int i = 0; i < n; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            linearStore.add(bounds);
        }
    }

    @Setup(Level.Invocation)
    public void clear(){
        QuadTree = new QuadTree<>(worldBounds);
    }

    @Benchmark
    public void QuadTree(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            QuadTree.add(i, linearStore.get(i));
        }
        bh.consume(QuadTree);
    }

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeAddBenchmark.class);
    }
}
