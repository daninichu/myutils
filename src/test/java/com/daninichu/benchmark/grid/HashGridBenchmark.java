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
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(		iterations = 2,	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 4, time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	HashGrid2<Integer> setGrid3;
    HashGrid2<Integer> filledGrid3;
	HashGrid<Integer> setGrid2;
    HashGrid<Integer> filledGrid2;
	int n = 10000000;
	int initCapGet = 2;

	int[] x, y;
//	Grid.Point[] points;

	@Setup
	public void setup() {
		filledGrid3 = new HashGrid2<>(initCapGet);
		filledGrid2 = new HashGrid<>(initCapGet);
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

			filledGrid3.set(x, y, i);
			filledGrid2.set(x, y, i);
		}
	}
	@Setup(Level.Iteration)
	public void setup2(){
		setGrid3 = new HashGrid2<>();
		setGrid2 = new HashGrid<>();
	}

	@Benchmark
	public void setGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid3.set(x[i], y[i], i);
		}
		bh.consume(setGrid3);
	}
	@Benchmark
	public void setGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid2.set(x[i], y[i], i);
		}
		bh.consume(setGrid2);
	}

	@Benchmark
	public void successfulGetGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid3.get(x[i], y[i]));
		}
	}
	@Benchmark
	public void successfulGetGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2.get(x[i], y[i]));
		}
	}

	@Benchmark
	public void failedGetGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid3.get(y[i], x[i]+1));
		}
	}
	@Benchmark
	public void failedGetGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2.get(y[i], x[i]+1));
		}
	}

	@Benchmark
	public void removeGrid3(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid3.removePoint(x[i], y[i]));
		}
	}
	@Benchmark
	public void removeGrid2(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid2.removePoint(x[i], y[i]));
		}
	}
}
