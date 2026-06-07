package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
//		Mode.AverageTime,
		Mode.SampleTime,
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

	@Param("2000")
	int width;
	@Param("4000")
	int height;

	ArrayGrid<Object> arrayGrid;

	@Setup(Level.Trial)
	public void setup(){
		arrayGrid = new ArrayGrid<>(width, height);
	}

	@Setup(Level.Invocation)
	public void setup2(){
//		setupForClear();
		setupForFill();
	}

	public void setupForClear(){
		for(int y = 0; y < height; y += 10){
			for(int x = 0; x < width; x++){
//				arrayGrid.set(x,y,x+y*width);
			}
		}
	}
	@Benchmark
	public void clear(Blackhole bh){
		bh.consume(arrayGrid.size());
		arrayGrid.clear();
		bh.consume(arrayGrid);
		bh.consume(arrayGrid.size());
	}

	public void setupForFill(){
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				arrayGrid.removePoint(x, y);
			}
		}
//		Random rng = new Random();
//		for(int y = 0; y < height; y++){
//			for(int x = 0; x < width; x++){
//				if(rng.nextInt(2) == 0)
//                    arrayGrid.set(x, y, rng.nextInt(9));
//			}
//		}
	}
//	@Benchmark
	public void fill(Blackhole bh){
		bh.consume(arrayGrid.size());
//		for(int y = 0; y < height; y++){
//			arrayGrid.fillRow(y, 0);
//		}
		arrayGrid.fill(111);
		bh.consume(arrayGrid);
		bh.consume(arrayGrid.size());
	}

//	@Benchmark
	public void containsValue(Blackhole bh){
		bh.consume(arrayGrid.toArray());
	}
}