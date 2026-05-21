package com.daninichu.benchmark;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.daninichu.ShapeRendererDrawer;
import com.daninichu.util.Grid;
import com.daninichu.util.HashGrid;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class Main {
	public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);

//		new Lwjgl3Application(
//				new ShapeRendererDrawer(),
//				config
//		);
	}

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