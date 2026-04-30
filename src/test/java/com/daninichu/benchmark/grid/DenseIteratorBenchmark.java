package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.AverageTime,
//        Mode.SampleTime
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 50, time = 1000000, timeUnit = TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Thread)
public class DenseIteratorBenchmark{
    public static void main(String[] args) throws Exception{
        Main.benchmark(DenseIteratorBenchmark.class);
    }
    int width = 2000;
    int height = 4000;

    ArrayGrid<Integer> arrayGrid = new ArrayGrid<>(width, height);
    HashGrid<Integer> spatialGrid = new HashGrid<>();

    {
        fill(arrayGrid);
        fill(spatialGrid);
    }

    public void fill(Grid<Integer> grid) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid.set(x, y, x + y);
            }
        }
    }

    @Benchmark
    public int arrayPoints() {
        int sum = 0;
        for(var _ : arrayGrid.points()){
            sum++;
        }
        return sum;
    }
    @Benchmark
    public int arrayValues() {
        int sum = 0;
        for(var _ : arrayGrid){
            sum++;
        }
        return sum;
    }
    @Benchmark
    public int arrayCells() {
        int sum = 0;
        for(var _ : arrayGrid.cells()){
            sum++;
        }
        return sum;
    }

//    @Benchmark
//    public int spatialPoints() {
//        int sum = 0;
//        for(var _ : spatialGrid.points()){
//            sum++;
//        }
//        return sum;
//    }
//    @Benchmark
//    public int spatialValues() {
//        int sum = 0;
//        for(var _ : spatialGrid){
//            sum++;
//        }
//        return sum;
//    }
//    @Benchmark
//    public int spatialCells() {
//        int sum = 0;
//        for(var _ : spatialGrid.cells()){
//            sum++;
//        }
//        return sum;
//    }
}