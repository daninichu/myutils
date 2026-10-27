package com.daninichu.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

class StopwatchTest{
    @Test
    void newStopwatchStartsRunning(){
        var stopwatch = new Stopwatch();

        assertFalse(stopwatch.isPaused());
        assertTrue(stopwatch.nanosAsLong() > 0);
        assertTrue(stopwatch.lapNanosAsLong() > 0);
    }

    @Test
    void newStopwatchStartsAtZero(){
        var stopwatch = new Stopwatch(true);

        assertTrue(stopwatch.isPaused());
        assertEquals(0, stopwatch.nanosAsLong());
        assertEquals(0, stopwatch.lapNanosAsLong());
    }

    @Test
    void constructorSetsInitialElapsedTime(){
        var stopwatch = new Stopwatch(1_000_000_000L, true);

        assertEquals(1_000_000_000L, stopwatch.nanosAsLong());
        assertEquals(0, stopwatch.lapNanosAsLong());

        stopwatch = new Stopwatch(-5000, true);

        assertEquals(-5000, stopwatch.nanosAsLong());
        assertEquals(0, stopwatch.lapNanosAsLong());
    }

    @Test
    void lapSums() throws InterruptedException{
        long[] laps = new long[100];
        var stopwatch = new Stopwatch();

        for(int i = 0; i < laps.length; i++){
            stopwatch.unpause();
            sleep(1);
            stopwatch.pause();
            laps[i] = stopwatch.newLapNanosAsLong();
        }

        assertEquals(stopwatch.nanosAsLong(), Arrays.stream(laps).sum());
    }

    @Test
    void pausedStopwatchDoesNotAdvance() throws InterruptedException{
        var stopwatch = new Stopwatch();
        stopwatch.pause();
        long time = stopwatch.nanosAsLong();
        long lap = stopwatch.lapNanosAsLong();
        sleep(50);

        assertEquals(time, stopwatch.nanosAsLong());
        assertEquals(lap, stopwatch.lapNanosAsLong());
    }

    @Test
    void resetLapPreservesPause() throws InterruptedException{
        var stopwatch = new Stopwatch();
        sleep(30);
        stopwatch.pause();

        long time = stopwatch.nanosAsLong();

        stopwatch.resetLap();

        assertTrue(stopwatch.isPaused());
        assertEquals(time, stopwatch.nanosAsLong());
        assertEquals(0, stopwatch.lapNanosAsLong());
    }

    @Test
    void resetLapThenResetStopwatch(){
        int nanos = 1_000_000_000;
        var stopwatch = new Stopwatch(nanos);

        assertTrue(stopwatch.lapNanosAsLong() > 0);

        stopwatch.pause();
        stopwatch.resetLap();

        assertTrue(stopwatch.nanosAsLong() > nanos);
        assertEquals(0, stopwatch.lapNanosAsLong());

        stopwatch.reset();

        assertTrue(stopwatch.isPaused());
        assertEquals(0, stopwatch.nanosAsLong());
        assertEquals(0, stopwatch.lapNanosAsLong());
    }

    @Test
    void newLapResetsLap() throws InterruptedException{
        var stopwatch = new Stopwatch();
        int millis = 30;

        sleep(millis);
        long lap = stopwatch.newLapMillisAsLong();

        assertTrue(lap >= millis);
        assertTrue(stopwatch.lapMillisAsLong() < millis);

        sleep(millis);

        assertTrue(stopwatch.lapMillisAsLong() >= millis);
    }
}
