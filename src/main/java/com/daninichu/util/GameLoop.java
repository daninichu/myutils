package com.daninichu.util;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

public class GameLoop implements Runnable{
    private final int fps;
    private final Runnable runnable;
    private Thread thread;
    private volatile boolean running;

    public GameLoop(int fps, Runnable runnable){
        if(fps <= 0)
            throw new IllegalArgumentException("fps must be positive");
        this.fps = fps;
        this.runnable = Objects.requireNonNull(runnable);
    }

    public synchronized void start(){
        if(thread != null)
            throw new IllegalStateException("GameLoop already started");
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    @Override
    public void run(){
        double frameTime = 1_000_000_000.0 / fps;
        double accumulator = 0;
        long last = System.nanoTime();
        while(running){
            long now = System.nanoTime();
            accumulator += now - last;
            last = now;
            if(accumulator >= frameTime){
                accumulator -= frameTime;
                runnable.run();
            }
            if(accumulator < frameTime)
                LockSupport.parkNanos((long) (frameTime - accumulator));
        }
    }
}