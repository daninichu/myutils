package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 4, time = 1)
@Fork(1)
public class ArrayBenchmark{
    public static void main(String[] args) throws Exception{
        Main.benchmark(ArrayBenchmark.class);
    }

    @Param("1000000")
    int n;
    Integer[] arr;

    @Setup(Level.Invocation)
    public void setup(){
        arr = new Integer[n];
        for (int i = 0; i < n; i++){
            arr[i] = i;
        }
    }

    @Benchmark
    public void fill(Blackhole bh){
        var arr = this.arr;
        Arrays.fill(arr, null);
        bh.consume(arr);
    }
    @Benchmark
    public void noNullCheck(Blackhole bh){
        var arr = this.arr;
        for(int i = 0; i < n; i++){
            arr[i] = null;
        }
        bh.consume(arr);
    }
    @Benchmark
    public void nullCheck(Blackhole bh){
        var arr = this.arr;
        for(int i = 0; i < n; i++){
            if(arr[i] != null)
                arr[i] = null;
        }
        bh.consume(arr);
    }
}