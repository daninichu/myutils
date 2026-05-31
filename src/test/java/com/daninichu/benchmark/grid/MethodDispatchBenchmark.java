package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MethodDispatchBenchmark {
    public static void main(String[] args) throws Exception{
        Main.benchmark(MethodDispatchBenchmark.class);
    }

    interface MyInterface {
        int compute(int x);
    }

    static final class FinalClass implements MyInterface {
        public int compute(int x) { return x * 2 + 1; }
    }

    private final MyInterface finalClass = new FinalClass();

    // No capture — effectively a final class in disguise
    private final MyInterface anonNoCapture = new MyInterface() {
        @Override
        public int compute(int x) { return x * 2 + 1; }
    };

    // With capture — adds a synthetic field
    private final int multiplier = 2;
    private final MyInterface anonWithCapture = new MyInterface() {
        @Override
        public int compute(int x) { return x * multiplier + 1; }
    };

    @Benchmark
    public int finalClassMethod() {
        return finalClass.compute(42);
    }

    @Benchmark
    public int anonNoCapture() {
        return anonNoCapture.compute(42);
    }

    @Benchmark
    public int anonWithCapture() {
        return anonWithCapture.compute(42);
    }
}