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

    int width = 100;
    int height = 1000;
    int n = width * height;
    Object[] data;
    Object[][] data2D;

    @Setup(Level.Iteration)
    public void setup(){
        data = new Object[n];
        data2D = new Object[height][width];
    }

    @Benchmark
    public void fill_0(Blackhole bh){
        for(int i = 0; i < n; i++){
            data[i] = 0;
        }
        bh.consume(data);
    }
    @Benchmark
    public void fill_128(Blackhole bh){
        for(int i = 0; i < n; i++){
            data[i] = 128;
        }
        bh.consume(data);
    }

    @Benchmark
    public void fill2D_0(Blackhole bh){
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                data2D[y][x] = 0;
            }
        }
        bh.consume(data2D);
    }
    @Benchmark
    public void fill2D_128(Blackhole bh){
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                data2D[y][x] = 128;
            }
        }
        bh.consume(data2D);
    }
}
