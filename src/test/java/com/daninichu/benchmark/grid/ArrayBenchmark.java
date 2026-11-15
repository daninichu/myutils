package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
//		Mode.AverageTime,
        Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ArrayBenchmark{
    public static void main(String[] args) throws Exception{
        Main.benchmark(ArrayBenchmark.class);
    }

    int n = 2000000;
    Object[] data;

    @Setup(Level.Trial)
    public void setup(){
        data = new Object[n];
    }

    @Benchmark
    public void fillValue(Blackhole bh){
        Object value = new Object();
        for(int i = 0; i < n; i++){
            data[i] = value;
        }
        bh.consume(data);
    }
    @Benchmark
    public void fillValue2(Blackhole bh){
        for(int i = 0; i < n; i++){
            Object value = new Object();
            data[i] = value;
        }
        bh.consume(data);
    }
}
