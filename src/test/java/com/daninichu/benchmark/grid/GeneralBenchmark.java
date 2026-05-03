package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;

import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.AverageTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000000000, timeUnit = TimeUnit.NANOSECONDS)
@Measurement(iterations = 5, time = 1000000000, timeUnit = TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Thread)
public class GeneralBenchmark{
//    Collection
    List
            <Integer> collection1, collection2;

    int n = 142500;

    @Setup(Level.Iteration)
    public void setAll() {
        collection1 = new ArrayList<>();
        collection2 = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            collection1.add(i);
            collection2.add(i);
        }
    }

    @Benchmark
    public void collection1(Blackhole bh) {
        for (int i = 0; i < collection1.size(); i++) {
            collection1.remove(i);
            i--;
        }
        Assertions.assertTrue(collection1.size() == 0);
        bh.consume(collection1);
    }
    @Benchmark
    public void collection2(Blackhole bh) {
        for (int i = 0; i < collection2.size(); i++) {
            Collections.swap(collection2, i, collection2.size() - 1);
            collection2.remove(collection2.size() - 1);
            i--;
        }
        Assertions.assertTrue(collection2.size() == 0);
        bh.consume(collection2);
    }

    public static void main(String[] args) throws Exception{
        Main.benchmark(GeneralBenchmark.class);
    }
}
