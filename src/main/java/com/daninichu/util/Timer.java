package com.daninichu.util;

import static java.lang.System.nanoTime;
import java.text.DecimalFormat;

public class Timer{
	private long start = nanoTime();

	public void reset() {
		start = nanoTime();
	}

	public double seconds(){
		return (nanoTime() - start) / 1_000_000_000.0;
	}

	public double milliseconds(){
		return (nanoTime() - start) / 1_000_000.0;
	}

	public double microseconds(){
		return (nanoTime() - start) / 1_000.0;
	}

	public double nanoseconds(){
		return nanoTime() - start;
	}

	public String secondsString(int decimals){
		DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimals));
		return df.format(seconds()) + "s";
	}

	public String millisecondsString(int decimals){
		DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimals));
		return df.format(milliseconds()) + "ms";
	}

	public String microsecondsString(int decimals){
		DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimals));
		return df.format(microseconds()) + "µs";
	}

	public String nanosecondsString(int decimals){
		DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimals));
		return df.format(nanoseconds()) + "ns";
	}

	public String secondsString(){
		return seconds() + "s";
	}

	public String millisecondsString(){
		return milliseconds() + "ms";
	}

	public String microsecondsString(){
		return microseconds() + "µs";
	}

	public String nanosecondsString(){
		return nanoseconds() + "ns";
	}

	@Override
	public String toString(){
		return secondsString();
	}
}
