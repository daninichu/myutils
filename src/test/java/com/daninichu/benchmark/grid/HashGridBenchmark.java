package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.HashGrid;
import com.daninichu.util.HashGrid4;
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

	HashGrid4<Integer> setGrid4;
    HashGrid4<Integer> filledGrid4;
	HashGrid<Integer> setGrid;
    HashGrid<Integer> filledGrid;
	int n = 10000000;
	int initCapGet = 2;

	int[] x, y;

	@Setup
	public void setup() {
		filledGrid4 = new HashGrid4<>(initCapGet);
		filledGrid = new HashGrid<>(initCapGet);
		x = new int[n];
		y = new int[n];

		Random rng = new Random(0);
		for (int i = 0; i < n; i++) {
			int x = rng.nextInt(-n, n);
			int y = rng.nextInt(-n, n);

			this.x[i] = x;
			this.y[i] = y;

			filledGrid4.set(x, y, i);
			filledGrid.set(x, y, i);
		}
	}
	@Setup(Level.Iteration)
	public void setup2(){
		setGrid4 = new HashGrid4<>(n*3/2);
		setGrid = new HashGrid<>(n*3/2);
	}

	@Benchmark
	public void setGrid4(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid4.set(x[i], y[i], i);
		}
		bh.consume(setGrid4);
	}
	@Benchmark
	public void setGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			setGrid.set(x[i], y[i], i);
		}
		bh.consume(setGrid);
	}

	@Benchmark
	public void successfulGetGrid4(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid4.get(x[i], y[i]));
		}
	}
	@Benchmark
	public void successfulGetGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid.get(x[i], y[i]));
		}
	}

	@Benchmark
	public void failedGetGrid4(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid4.get(y[i], x[i]+1));
		}
	}
	@Benchmark
	public void failedGetGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid.get(y[i], x[i]+1));
		}
	}

	@Benchmark
	public void removeGrid4(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid4.removePoint(x[i], y[i]));
		}
	}
	@Benchmark
	public void removeGrid(Blackhole bh) {
		for (int i = 0; i < n; i++) {
			bh.consume(filledGrid.removePoint(x[i], y[i]));
		}
	}
}
