package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.geom.Rectangle2D;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.AverageTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(        iterations = 2,     time = 1000,    timeUnit = TimeUnit.MILLISECONDS)
@Measurement(   iterations = 3,     time = 1000,    timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeIteratorBenchmark{

    @Param({
            "1000000",
            "2000000",
            "3000000",
    })
    int n;

    @Param({
            "8",
    })
    int maxDepth;

    double WORLD = 100000;
    double ELEMENT_SIZE = 0;
    Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    QuadTree<Integer> QuadTree;

    @Setup(Level.Trial)
    public void setUp() {
        this.QuadTree = new QuadTree<>(worldBounds, maxDepth);

        Random rng = new Random(42);

        for (int i = 0; i < n; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);

            this.QuadTree.add(i, x, y, ELEMENT_SIZE, ELEMENT_SIZE);
        }
    }

    @Benchmark
    public void iterator(Blackhole bh){
        for(Integer i : this.QuadTree){
            bh.consume(i);
        }
    }
    @Benchmark
    public void iterator2(Blackhole bh){
        for(var i : this.QuadTree.entries()){
            bh.consume(i);
        }
    }

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeIteratorBenchmark.class);
    }
}
