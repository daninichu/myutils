package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.IterativeQuadTree;
import com.daninichu.util.QuadTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.AverageTime,
//        Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(        iterations = 2,     time = 1000,    timeUnit = TimeUnit.MILLISECONDS)
@Measurement(   iterations = 5,     time = 1000,    timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeIteratorBenchmark{

    @Param({
            "1000000",
    })
    public int elementCount;

    private static final double WORLD = 1000000;
    private static final double ELEMENT_SIZE = 0;
    private static final int MAX_DEPTH = 8;
    private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    private final QuadTree<Integer> QuadTree = new QuadTree<>(worldBounds, MAX_DEPTH);

    @Setup(Level.Trial)
    public void setUp() {
        Random rng = new Random(42);

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            Integer e = i;
            this.QuadTree.add(e, bounds);
        }
    }

    @Benchmark
    public void QuadTree(Blackhole bh){
        for(Integer i : this.QuadTree){
            bh.consume(i);
        }
    }

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeIteratorBenchmark.class);
    }
}
