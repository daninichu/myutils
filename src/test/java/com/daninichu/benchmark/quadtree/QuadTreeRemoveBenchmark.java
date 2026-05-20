package com.daninichu.benchmark.quadtree;

import com.daninichu.benchmark.Main;
import com.daninichu.util.QuadTree;
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
            "2000000",
//            "200000",
    })
    public int n;
    @Param({
            "1000",
//            "200",
    })
    public int removeCount;

    double WORLD = 100000;
    double ELEMENT_SIZE = 0;
    double maxSpeed = 20;
    int MAX_DEPTH = 8;
    Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);

    QuadTree<Integer> QuadTree;
    ArrayList<QuadTree.Entry<Integer>> entries;

    @Setup(Level.Invocation)
    public void setup() {
        entries = new ArrayList<>(n);
        this.QuadTree  = new QuadTree<>(worldBounds, MAX_DEPTH);

        Random rng = new Random(42);

        for (int i = 0; i < n; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            QuadTree.Entry<Integer> entry = QuadTree.add(i, bounds);
            entries.add(entry);
        }
    }

    @Benchmark
    public void QuadTree(Blackhole bh) {
        Random rng = new Random(42);
        for(QuadTree.Entry<Integer> entry : entries) {
            double x = entry.getX() + (rng.nextDouble() - 0.5) * maxSpeed;
            double y = entry.getY() + (rng.nextDouble() - 0.5) * maxSpeed;
            double w = entry.getWidth();
            double h = entry.getHeight();
//            x = Math.min(x, WORLD - w);
//            x = Math.max(x, 0);
//            y = Math.min(y, WORLD - h);
//            y = Math.max(y, 0);
            this.QuadTree.remove(entry);
            this.QuadTree.add(entry.value, x, y, w, h);
        }
        bh.consume(this.QuadTree);
    }

    @Benchmark
    public void move(Blackhole bh) {
        Random rng = new Random(42);
        for(QuadTree.Entry<Integer> entry : entries) {
            double x = entry.getX() + (rng.nextDouble() - 0.5) * maxSpeed;
            double y = entry.getY() + (rng.nextDouble() - 0.5) * maxSpeed;
            double w = entry.getWidth();
            double h = entry.getHeight();
//            x = Math.min(x, WORLD - w);
//            x = Math.max(x, 0);
//            y = Math.min(y, WORLD - h);
//            y = Math.max(y, 0);
            this.QuadTree.move(entry, x, y, w, h);
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
