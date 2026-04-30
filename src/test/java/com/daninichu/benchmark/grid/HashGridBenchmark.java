package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import org.openjdk.jmh.annotations.*;

import java.awt.Point;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 100, time = 1, timeUnit = TimeUnit.NANOSECONDS)
//@Warmup(iterations = 3, time = 1)
//@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	HashGrid<Integer> grid;
	int n = 2000;
	{
		grid = new HashGrid<>();
		fill();
	}

	public void fill() {
		for (int i = 0; i < n; i++) {
			grid.set(new Point(i, i), i);
		}
	}

	@Benchmark
	public int points() {
		int sum = 0;
		for(Point p : grid.points()){
			sum += p.x;
		}
		return sum;
	}

	@Benchmark
	public int cells() {
		int sum = 0;
		for(Grid.Cell<Integer> p : grid.cells()){
			sum++;
		}
		return sum;
	}
}
