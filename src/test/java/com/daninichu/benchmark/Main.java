package com.daninichu.benchmark;

import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class Main {
	public static void benchmark(Class<?> benchmarkClass) throws Exception {
		Options opt = new OptionsBuilder()
				.include(benchmarkClass.getName())
				.build();

		new Runner(opt).run();
	}

	public static void profile(Class<?> benchmarkClass) throws Exception {
		Options opt = new OptionsBuilder()
				.include(benchmarkClass.getName())
				.addProfiler(GCProfiler.class)
				.build();

		new Runner(opt).run();
	}
}