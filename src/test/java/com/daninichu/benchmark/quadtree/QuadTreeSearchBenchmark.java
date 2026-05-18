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
@Warmup(        iterations = 2,     time = 1900,    timeUnit = TimeUnit.MILLISECONDS)
@Measurement(   iterations = 5,     time = 1900,    timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QuadTreeSearchBenchmark{

    @Param({
            "800000",
    })
    public int elementCount;

    @Param({
//            "0.01",
//            "0.20",
            "1",
    })
    public double fraction;


    private static final double WORLD = 1000000;
    private static final double ELEMENT_SIZE = 0;
    private static final int MAX_DEPTH = 8;
    private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD, WORLD);


    private final QuadTree<Integer> QuadTree = new QuadTree<>(worldBounds, MAX_DEPTH);
    private final IterativeQuadTree<Integer> IterativeQuadTree = new IterativeQuadTree<>(worldBounds, MAX_DEPTH);

    private final Collection<Object> entriesQuadTree = new ArrayList<>(elementCount);
//    private final Collection<? super IterativeQuadTree.Entry<Integer>> entriesIterativeQuadTree = new ArrayList<>(elementCount);

    private Rectangle2D searchArea;

    @Setup(Level.Trial)
    public void setUp() {
        Random rng = new Random(42);

        for (int i = 0; i < elementCount; i++) {
            double x = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            double y = rng.nextDouble() * (WORLD - ELEMENT_SIZE);
            Rectangle2D bounds = new Rectangle2D.Double(x, y, ELEMENT_SIZE, ELEMENT_SIZE);

            Integer e = i;
            this.QuadTree.add(e, bounds);
            this.IterativeQuadTree.add(e, bounds);
        }

        double side = WORLD * Math.sqrt(fraction);   // square centred in the world
        double offset = (WORLD - side) / 2.0;
        searchArea = new Rectangle2D.Double(offset, offset, side, side);
    }

    @Setup(Level.Invocation)
    public void clear(){
        entriesQuadTree.clear();
//        entriesIterativeQuadTree.clear();
    }

    @Benchmark
    public void QuadTree(Blackhole bh){
        this.QuadTree.searchEntries(searchArea, entriesQuadTree);
        bh.consume(entriesQuadTree);
    }

//    @Benchmark
    public void IterativeQuadTree(Blackhole bh){
        this.IterativeQuadTree.searchEntries(searchArea, entriesQuadTree);
        bh.consume(entriesQuadTree);
    }

    public static void main(String[] args) throws Exception{
        Main.benchmark(QuadTreeSearchBenchmark.class);
    }
}
