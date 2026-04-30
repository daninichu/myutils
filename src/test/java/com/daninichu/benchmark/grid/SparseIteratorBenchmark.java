package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import org.openjdk.jmh.annotations.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
        Mode.AverageTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.NANOSECONDS)
@Measurement(iterations = 100, time = 1, timeUnit = TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Thread)
public class SparseIteratorBenchmark{
    public static void main(String[] args) throws Exception{
        Main.benchmark(SparseIteratorBenchmark.class);
    }

    int width = 2000;
    int height = 400;
    ArrayGrid<Integer> arrayGrid = new ArrayGrid<>(width, height);
    HashGrid<Integer> spatialGrid = new HashGrid<>();

    @Setup(Level.Trial)
    public void setAll() {
        List<Grid.Cell<Integer>> cells = new ArrayList<>();

        int n = 100;
//        int n = width;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells.add(new Grid.Cell<>(new Point(x, y), x + y));
            }
        }
        Collections.shuffle(cells);
        cells = cells.subList(0, n);

        for (var cell : cells) {
            arrayGrid.set(cell.point(), cell.value());
            spatialGrid.set(cell.point(), cell.value());
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

    @Benchmark
    public int spatialPoints() {
        int sum = 0;
        for(var _ : spatialGrid.points()){
            sum++;
        }
        return sum;
    }
    @Benchmark
    public int spatialValues() {
        int sum = 0;
        for(var _ : spatialGrid){
            sum++;
        }
        return sum;
    }
    @Benchmark
    public int spatialCells() {
        int sum = 0;
        for(var _ : spatialGrid.cells()){
            sum++;
        }
        return sum;
    }
}