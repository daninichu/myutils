package com.daninichu.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {
	public static void benchmark(Class<?> benchmarkClass) throws Exception {
		Options opt = new OptionsBuilder()
				.include(benchmarkClass.getName())
				.build();

		new Runner(opt).run();
	}
}