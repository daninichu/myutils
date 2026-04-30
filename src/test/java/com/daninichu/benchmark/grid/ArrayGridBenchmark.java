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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(		iterations = 2, 	time = 900000000, 	timeUnit = TimeUnit.NANOSECONDS)
@Measurement(	iterations = 5, 	time = 900000000, 	timeUnit = TimeUnit.NANOSECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	int width =
			1;
	int height =
			200000;

	ArrayGrid<Integer> arrayGrid = new ArrayGrid<>(width, height);

	@Setup(Level.Trial)
	public void setup() {
//		var cells = GridUtils.cells(width, height);
//		Collections.shuffle(cells);
//		cells = cells.subList(0, 800000);
//		GridUtils.setAll(arrayGrid, cells);
//		GridUtils.fill(arrayGrid, width, height);
	}

	@Benchmark
	public void arrayGrid(Blackhole bh){
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				arrayGrid.set(x, y, x+y);
			}
		}
	}
}