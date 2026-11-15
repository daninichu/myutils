package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
//		Mode.AverageTime,
		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(		iterations = 2, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 3, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	int width = 1000;
	int height = 100;

	ArrayGrid<Object> arrayGrid;

	@Setup(Level.Trial)
	public void setup(){
		arrayGrid = new ArrayGrid<>(width, height);
	}

	@Benchmark
	public void a(Blackhole bh){
		f(arrayGrid, bh);
	}

	@Benchmark
	public void fa(Blackhole bh){
	}

	void f(Grid<Object> grid, Blackhole bh){
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
//				bh.consume(grid.get(x, y));
				grid.set(x, y, 999);
			}
		}
		bh.consume(grid);
	}
}
