package com.daninichu.benchmark;

import com.daninichu.util.Grid;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class Main {
	public static void main(String[] args){
		Grid.Cell cell = new Grid.Cell<>(9358,50, 9);
		System.out.println(cell);

	}
	public static void benchmark(Class<?> benchmarkClass) throws Exception {
		Options opt = new OptionsBuilder()
				.include(benchmarkClass.getName())
				.build();

		new Runner(opt).run();
	}
}