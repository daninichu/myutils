package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import com.daninichu.util.HashGrid2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
//		Mode.AverageTime,
		Mode.SampleTime,
//		Mode.Throughput,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	HashGrid<Integer> grid;
	HashGrid2<Integer> grid2;
	int n = 100000;

	int[] x, y;
	Grid.Point[] points1;
	Grid.Cell<Integer>[] cells;
	HashGrid2.Point[] points2;

	@Setup
	public void setup() {
		x = new int[n];
		y = new int[n];
		points1 = new Grid.Point[n];
		points2 = new HashGrid2.Point[n];
		cells = new Grid.Cell[n];

		Random rng = new Random();
		for (int i = 0; i < n; i++) {
			int x = rng.nextInt(-n, n);
			int y = rng.nextInt(-n, n);

			this.x[i] = x;
			this.y[i] = y;
			points1[i] = new Grid.Point(x, y);
			points2[i] = new HashGrid2.Point(x, y);
			cells[i] = new Grid.Cell<>(x, y, i);
		}
	}
	@Setup(Level.Invocation)
	public void setup2(){
		grid = new HashGrid<>(n*3/2);
		grid2 = new HashGrid2<>(n*3/2);
	}

	@Benchmark
	public void grid1(Blackhole bh) {
		for (int i = 0; i < n; i++) {
//			grid.set(x[i], y[i], i);
			grid.set(points1[i], i);
		}
		bh.consume(grid);
	}
	@Benchmark
	public void grid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
//			grid2.set(x[i], y[i], i);
			grid2.set(points2[i], i);
		}
		bh.consume(grid2);
	}

//	@Benchmark
//	public void point1(Blackhole bh) {
//		for (int i = 0; i < n; i++) {
//			bh.consume(points1[i].hashCode());
//		}
//	}
//	@Benchmark
//	public void point2(Blackhole bh) {
//		for (int i = 0; i < n; i++) {
//			bh.consume(points2[i].hashCode());
//		}
//	}
//	@Benchmark
//	public void cells(Blackhole bh) {
//		for (int i = 0; i < n; i++) {
//			bh.consume(cells[i].hashCode());
//		}
//	}
}
