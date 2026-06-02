package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.HashGrid;
import com.daninichu.util.HashGrid2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
//		Mode.Throughput,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(		iterations = 2,	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 4, time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	HashGrid<Integer> grid;
    HashGrid<Integer> filledGrid;
	HashGrid2<Integer> setGrid2_5;
    HashGrid2<Integer> filledGrid2_5;
	HashGrid2<Integer> setGrid2;
    HashGrid2<Integer> filledGrid2;
	int n = 1000000;
	int initCapGet = 2*n;

	int[] x, y;
//	Grid.Point[] points;

	@Setup
	public void setup() {
//		filledGrid = new HashGrid<>(initCapGet);
		filledGrid2_5 = new HashGrid2<>(initCapGet);
		filledGrid2 = new HashGrid2<>(initCapGet);
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
//			filledGrid.set(x, y, i);
			filledGrid2_5.set(x, y, i);
			filledGrid2.set(x, y, i);

//			Assertions.assertEquals(i+1, filledGrid2_5.size());
//			Assertions.assertEquals(i+1, filledGrid2.size());
		}
	}
	@Setup(Level.Iteration)
	public void setup2(){
		setGrid2_5 = new HashGrid2<>();
		setGrid2 = new HashGrid2<>();
	}

	@Benchmark
	public void setGrid2_5(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid2_5.set(x[i], y[i], i);
		}
		bh.consume(setGrid2_5);
	}
//	@Benchmark
	public void setGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid2.set(x[i], y[i], i);
		}
		bh.consume(setGrid2);
	}

//	@Benchmark
	public void successfulGetGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid.get(x[i], y[i]));
		}
	}
	@Benchmark
	public void successfulGetGrid2_5(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2_5.get(x[i], y[i]));
		}
	}
//	@Benchmark
	public void successfulGetGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2.get(x[i], y[i]));
		}
	}

	//	@Benchmark
	public void failedGetGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid.get(y[i], x[i]+1));
		}
	}
	@Benchmark
	public void failedGetGrid2_5(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2_5.get(y[i], x[i]+1));
		}
	}
//	@Benchmark
	public void failedGetGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2.get(y[i], x[i]+1));
		}
	}

//	@Benchmark
	public void removeGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid.removePoint(x[i], y[i]));
		}
	}
	@Benchmark
	public void removeGrid2_5(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2_5.removePoint(x[i], y[i]));
		}
	}
//	@Benchmark
	public void removeGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2.removePoint(x[i], y[i]));
		}
	}
}
