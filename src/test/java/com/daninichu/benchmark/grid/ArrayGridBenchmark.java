package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(		iterations = 2, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 4, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	int width = 1000000;
	int height = 40;

	ArrayGrid<Object> arrayGrid;

	@Setup(Level.Iteration)
	public void setup2(){
		arrayGrid.clear();
	}

	@Setup(Level.Trial)
	public void setup(){
		arrayGrid = new ArrayGrid<>(width, height);
	}

	@Benchmark
	public void arrayGrid(Blackhole bh){
		arrayGrid.fill(111);
		bh.consume(arrayGrid);
		if(arrayGrid.size() != width * height){
			throw new AssertionError();
		}
	}
	@Benchmark
	public void arrayGrid2(Blackhole bh){
		bh.consume(arrayGrid);
		if(arrayGrid.size() != width * height){
//			throw new AssertionError();
		}
	}
}