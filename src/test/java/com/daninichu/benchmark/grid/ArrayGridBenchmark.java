package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import com.daninichu.util.FlatArrayGrid;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({
		Mode.AverageTime,
//		Mode.SampleTime,
})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(		iterations = 2, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Measurement(	iterations = 5, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{

	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	@Param({
			"1000000",
	}) int n;
	@Param({
//			"0",
			"1",
//			"50",
			"99",
//			"100",
	})
	int widthPercent;

	int width, height;

	ArrayGrid<Object> arrayGrid;
	FlatArrayGrid<Object> flatArrayGrid;

	@Setup(Level.Iteration)
	public void setup2(){
		arrayGrid.clear();
		flatArrayGrid.clear();
	}
	@Setup(Level.Trial)
	public void setup(){
		if(widthPercent == 0){
			width = 1;
			height = n;
		} else if(widthPercent == 100){
			width = n;
			height = 1;
		} else{
			width = n * widthPercent / 100;
			height = n / width;
		}

		arrayGrid = new ArrayGrid<>(width, height);
		flatArrayGrid = new FlatArrayGrid<>(width, height);
	}

	@Benchmark
	public void arrayGrid(Blackhole bh){
		int i = 0;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
//				Object e = 0;
//				arrayGrid.set(x, y, i++);
				bh.consume(arrayGrid.get(x, y));
			}
		}
		bh.consume(arrayGrid);
	}
	@Benchmark
	public void flatArrayGrid(Blackhole bh){
		int i = 0;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
//				Object e = 0;
//				flatArrayGrid.set(x, y, i++);
				bh.consume(flatArrayGrid.get(x, y));
			}
		}
		bh.consume(flatArrayGrid);
	}
}