package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(		iterations = 2, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 5, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{

	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	@Param({
//			"100",
			"2000000",
//			"4000",
	})
	int width;
	@Param({
//			"1000",
			"1",
//			"40000",
	})
	int height;

	@Benchmark
	public void arrayGrid(Blackhole bh){
		var arrayGrid = new ArrayGrid<Integer>(width, height);

		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				arrayGrid.set(x, y, x+y);
			}
		}

		bh.consume(arrayGrid);
	}
}