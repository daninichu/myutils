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
@Warmup(		iterations = 3, 	time = 1000000000, 	timeUnit = TimeUnit.NANOSECONDS)
@Measurement(	iterations = 5, 	time = 1000000000, 	timeUnit = TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{

	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	@Param({
//			"100",
			"2000",
//			"4000",
	})	int width;
	@Param({
//			"1000",
			"2000",
//			"40000",
	})
	int height;

	private ArrayGrid<Integer> arrayGrid;

	@Setup(Level.Trial)
	public void setup() {
		arrayGrid = new ArrayGrid<>(width, height);
		var cells = GridUtils.cells(width, height);
		Collections.shuffle(cells);
		cells = cells.subList(0, 8);
		GridUtils.setAll(arrayGrid, cells);
//		GridUtils.fill(arrayGrid, width, height);
	}

//	@Benchmark
	public void arrayGrid(Blackhole bh){
//		var arrayGrid = new ArrayGrid<Integer>(width, height);
//		for(int x = 0; x < width; x++){
//			for(int y = 0; y < height; y++){
//				arrayGrid.set(x, y, x+y);
//			}
//		}

		bh.consume(arrayGrid.containsValue(-1));
	}
}