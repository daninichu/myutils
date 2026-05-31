package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
//		Mode.Throughput,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(		iterations = 2,	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 5, time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	HashGrid<Integer> grid;

	int n = 1000000;

	int[] x, y;
	Grid.Point[] points1;

	@Setup
	public void setup() {
		x = new int[n];
		y = new int[n];
		points1 = new Grid.Point[n];

		Random rng = new Random();
		for (int i = 0; i < n; i++) {
			int x = rng.nextInt(-n, n);
			int y = rng.nextInt(-n, n);

			this.x[i] = x;
			this.y[i] = y;
			points1[i] = new Grid.Point(x, y);
		}
	}
	@Setup(Level.Iteration)
	public void setup2(){
		grid = new HashGrid<>(n*3/2);
	}

	@Benchmark
	public void grid1(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			grid.get(points1[i]);
		}
		bh.consume(grid);
	}
}
