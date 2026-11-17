package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
//		Mode.AverageTime,
		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(		iterations = 2, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 3, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	int width = 10000;
	int height = 3019;

	HashGrid<Object> grid;

	@Setup(Level.Trial)
	public void setup(){
		float f=0.9f;
		int n = 1<<25;
		n*=f;
		height=n/width;
		grid = new HashGrid<>(n, f);
	}

	@Benchmark
	public void a(Blackhole bh){
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				grid.set(x, y, null);
			}
		}
//		System.out.println("size= "+grid.size());
//		System.out.println("threshold= "+grid.threshold);
//		Map<Integer, Integer> probeMap = new HashMap<>();
//		for(var i : grid.distances){
//			probeMap.put(i, probeMap.getOrDefault(i, 0) + 1);
//		}
//		System.out.println(probeMap);
//		System.out.println();
	}
}
