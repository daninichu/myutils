package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
public class QuadTreeRemoveBenchmark{
    @Param({
            "100000",
//            "200000",
    })
    public int elementCount;
    @Param({
            "1000",
//            "200",
    })
    public int removeCount;

    double WORLD = 100000;
    double ELEMENT_SIZE = 0;
    int MAX_DEPTH = 8;
    Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    QuadTree<Integer> QuadTree;
    ArrayList<QuadTree.Entry<Integer>> toRemoveQuadTree = new ArrayList<>(elementCount);

    @Setup(Level.Invocation)
    public void setup() {
        toRemoveQuadTree.clear();
        this.QuadTree  = new QuadTree<>(worldBounds, MAX_DEPTH);

        Random rng = new Random(42);

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            QuadTree.Entry<Integer> entry = QuadTree.add(i, bounds);
            toRemoveQuadTree.add(entry);
        }
    }

    @Benchmark
    public void QuadTree(Blackhole bh) {
        for(int i = 0; i < removeCount; i++){
            QuadTree.Entry<Integer> entry = toRemoveQuadTree.get(i);
            Assertions.assertTrue(this.QuadTree.removeAndCollapse(entry.value));
        }
        bh.consume(this.QuadTree);
    }

    // -------------------------------------------------------------------------
    // IDE entry point (quick smoke-run, not for real measurements)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeRemoveBenchmark.class);
    }
}
