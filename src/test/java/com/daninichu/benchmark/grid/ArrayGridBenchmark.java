package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import com.daninichu.util.ArrayGrid;
import com.daninichu.util.FlatArrayGrid;
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
@Measurement(	iterations = 3, 	time = 1000, 	timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class ArrayGridBenchmark{
	public static void main(String[] args) throws Exception{
		Main.benchmark(ArrayGridBenchmark.class);
	}

	int width = 2000;
	int height = 400;

	ArrayGrid<Object> arrayGrid;
	FlatArrayGrid<Object> flatArrayGrid;

	@Setup(Level.Trial)
	public void setup(){
		arrayGrid = new ArrayGrid<>(width, height);
		flatArrayGrid = new FlatArrayGrid<>(width, height);
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				flatArrayGrid.set(x, y, x);
				arrayGrid.set(x, y, x);
			}
		}
	}

	@Benchmark
	public void a(Blackhole bh){
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
//				bh.consume(arrayGrid.get(x, y));
				bh.consume(arrayGrid.set(x, y, x));
			}
		}
//		for(var e : arrayGrid){
//			bh.consume(e);
//		}
		bh.consume(arrayGrid);
	}

	@Benchmark
	public void fa(Blackhole bh){
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
//				bh.consume(flatArrayGrid.get(x, y));
				bh.consume(flatArrayGrid.set(x, y, x));
			}
		}
//		for(var e : flatArrayGrid){
//			bh.consume(e);
//		}
		bh.consume(flatArrayGrid);
	}
}
