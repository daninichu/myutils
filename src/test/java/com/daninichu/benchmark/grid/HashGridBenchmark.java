package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import com.daninichu.util.HashGrid2;
import com.daninichu.util.HashGrid3;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
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

//	HashGrid<Integer> grid, getGrid;
	HashGrid2<Integer> setGrid2;
    HashGrid2<Integer> getGrid2;
	HashGrid3<Integer> setGrid3;
    HashGrid3<Integer> getGrid3;
	int n = 100000;

	int[] x, y;
//	Grid.Point[] points;

	@Setup
	public void setup() {
//		getGrid = new HashGrid<>(n*3/2);
		getGrid2 = new HashGrid2<>();
		getGrid3 = new HashGrid3<>();
		x = new int[n];
		y = new int[n];
//		points = new Grid.Point[n];

		Random rng = new Random(0);
		for (int i = 0; i < n; i++) {
			int x = rng.nextInt(-n, n);
			int y = rng.nextInt(-n, n);

			this.x[i] = x;
			this.y[i] = y;
//			points[i] = new Grid.Point(x, y);
//			getGrid.set(x, y, null);
			getGrid2.set(x, y, i);
			getGrid3.set(x, y, i);

//			Assertions.assertEquals(i+1, getGrid2.size());
//			Assertions.assertEquals(i+1, getGrid3.size());
		}
	}
	@Setup(Level.Iteration)
	public void setup2(){
		setGrid2 = new HashGrid2<>(n);
		setGrid3 = new HashGrid3<>(n);
	}

//	@Benchmark
//	public void grid1(Blackhole bh) {
//		for (int i = 0; i < n; i++) {
//			grid.set(x[i], y[i], i);
////			grid.set(points[i], i);
//		}
//		bh.consume(grid);
//	}
	@Benchmark
	public void setGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid2.set(x[i], y[i], i);
		}
		bh.consume(setGrid2);
	}
	@Benchmark
	public void setGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid3.set(x[i], y[i], i);
		}
		bh.consume(setGrid3);
	}

//	@Benchmark
//	public void getGrid(Blackhole bh) {
//		for (int i = 0; i < n; i++) {
//			bh.consume(getGrid.get(x[i], y[i]));
//		}
//	}
	@Benchmark
	public void successfulGetGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(getGrid2.get(x[i], y[i]));
		}
	}
	@Benchmark
	public void successfulGetGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(getGrid3.get(x[i], y[i]));
		}
	}

	@Benchmark
	public void failedGetGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(getGrid2.get(y[i], x[i]+1));
		}
	}
	@Benchmark
	public void failedGetGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(getGrid3.get(y[i], x[i]+1));
		}
	}
}
