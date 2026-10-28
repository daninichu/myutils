package com.daninichu.util;

import static java.lang.System.nanoTime;

/**
 * Measures elapsed time using {@link System#nanoTime()}.
 * Supports tracking total elapsed time, measuring laps, and pausing and resuming.
 * <p>
 * This class is not thread-safe. Concurrent access requires external synchronization.
 */
public class Stopwatch{
	private long start, lapStart, pauseTime;
	private boolean paused;

	/**
	 * Creates a stopwatch with zero elapsed time, initially running.
	 */
	public Stopwatch(){
		start = lapStart = nanoTime();
	}

	/**
	 * Creates a stopwatch with a specified elapsed time, initially running.
	 * @param elapsedNanos the initial elapsed time in nanoseconds
	 */
	public Stopwatch(long elapsedNanos){
		start = -elapsedNanos + (lapStart = nanoTime());
	}

	/**
	 * Creates a stopwatch with zero elapsed time and the specified pause state.
	 * @param paused {@code true} to create the stopwatch paused;
	 *               {@code false} to create it running
	 */
	public Stopwatch(boolean paused){
		this.paused = paused;
		start = lapStart = pauseTime = nanoTime();
	}

	/**
	 * Creates a stopwatch with a specified elapsed time and pause state.
	 * @param elapsedNanos the initial elapsed time in nanoseconds
	 * @param paused {@code true} to create the stopwatch paused;
	 *               {@code false} to create it running
	 */
	public Stopwatch(long elapsedNanos, boolean paused){
		this.paused = paused;
		start = -elapsedNanos + (lapStart = pauseTime = nanoTime());
	}

	public void reset(){
		start = lapStart = pauseTime = nanoTime();
	}

	public void resetLap(){
		lapStart = paused? pauseTime : nanoTime();
	}

	public void pause(){
		if(!paused){
			paused = true;
			pauseTime = nanoTime();
		}
	}

	public void unpause(){
		if(paused){
			paused = false;
			long x = nanoTime() - pauseTime;
            start += x;
			lapStart += x;
		}
	}

	public boolean isPaused(){
		return paused;
	}

	public double sec(){
		return nanosAsLong() / 1_000_000_000.0;
	}
	public double millis(){
		return nanosAsLong() / 1_000_000.0;
	}
	public double micros(){
		return nanosAsLong() / 1_000.0;
	}
	public double nanos(){
		return nanosAsLong();
	}

	public long secAsLong(){
		return nanosAsLong() / 1_000_000_000;
	}
	public long millisAsLong(){
		return nanosAsLong() / 1_000_000;
	}
	public long microsAsLong(){
		return nanosAsLong() / 1_000;
	}
	public long nanosAsLong(){
		return (paused? pauseTime : nanoTime()) - start;
	}

	public double newLapSec(){
		return newLapNanosAsLong() / 1_000_000_000.0;
	}
	public double newLapMillis(){
		return newLapNanosAsLong() / 1_000_000.0;
	}
	public double newLapMicros(){
		return newLapNanosAsLong() / 1_000.0;
	}
	public double newLapNanos(){
		return newLapNanosAsLong();
	}

	public long newLapSecAsLong(){
		return newLapNanosAsLong() / 1_000_000_000;
	}
	public long newLapMillisAsLong(){
		return newLapNanosAsLong() / 1_000_000;
	}
	public long newLapMicrosAsLong(){
		return newLapNanosAsLong() / 1_000;
	}
	public long newLapNanosAsLong(){
		long lap = lapNanosAsLong();
		resetLap();
		return lap;
	}

	public double lapSec(){
		return lapNanosAsLong() / 1_000_000_000.0;
	}
	public double lapMillis(){
		return lapNanosAsLong() / 1_000_000.0;
	}
	public double lapMicros(){
		return lapNanosAsLong() / 1_000.0;
	}
	public double lapNanos(){
		return lapNanosAsLong();
	}

	public long lapSecAsLong(){
		return lapNanosAsLong() / 1_000_000_000;
	}
	public long lapMillisAsLong(){
		return lapNanosAsLong() / 1_000_000;
	}
	public long lapMicrosAsLong(){
		return lapNanosAsLong() / 1_000;
	}
	public long lapNanosAsLong(){
		return (paused? pauseTime : nanoTime()) - lapStart;
	}

	@Override
	public String toString(){
		long x = paused? pauseTime : nanoTime();
        double time = (x - start) / 1_000_000_000.0;
        double lap = (x - lapStart) / 1_000_000_000.0;
		return "Stopwatch[time=" + time + "s, lap=" + lap + "s]";
	}
}
