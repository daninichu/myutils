package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.HashGrid;
import com.daninichu.util.HashGrid2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
//@Warmup(iterations = 3, time = 1)
//@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class HashGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(HashGridBenchmark.class);
	}

	HashGrid<Integer> grid;
	HashGrid2<Integer> grid2;
	int n = 20000;
	{
		grid = new HashGrid<>(2*n);
		grid2 = new HashGrid2<>(2*n);
//		fill();
	}

	private List<Point> points;
	@Setup
	public void setup(){
		points = new ArrayList<>(n);

		Random rng = new Random(0);

		for(int i = 0; i < n; i++){
			int x = rng.nextInt(-n, n);
			int y = rng.nextInt(-n, n);
			points.add(new Point(x, y));
		}
	}
	@Setup(Level.Invocation)
	public void setup2(){
		grid = new HashGrid<>(2*n);
		grid2 = new HashGrid2<>(2*n);
	}

//	@Benchmark
	public void grid1(Blackhole bh) {
//		var grid = new HashGrid<>(2*n);

		for(Point p : points){
			grid.set(p.x,p.y,1);
		}
//		for (int i = 0; i < n; i++) {
//			grid.set(i, i, i);
//		}
		bh.consume(grid);
	}

	@Benchmark
	public void grid2(Blackhole bh) {
//		var grid2 = new HashGrid2<>(2*n);
		for(Point p : points){
			grid2.set(p.x,p.y,1);
		}
//		for (int i = 0; i < n; i++) {
//			grid2.set(i, i, i);
//		}
		bh.consume(grid2);
	}
}
