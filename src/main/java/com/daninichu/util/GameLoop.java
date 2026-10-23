package com.daninichu.util;

import java.util.Objects;
import java.util.function.DoubleConsumer;

public class GameLoop implements Runnable{
    private final double frameTime;
    private final DoubleConsumer runnable;
    private Thread thread;
    private volatile boolean running;

    public GameLoop(int fps, DoubleConsumer update){
        if(fps <= 0)
            throw new IllegalArgumentException("fps must be positive");
        frameTime = 1_000_000_000.0 / fps;
        this.runnable = Objects.requireNonNull(update);
    }

    public synchronized void start(){
        if(thread != null)
            throw new IllegalStateException("GameLoop already started");
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    @Override
    public void run(){
        long last = System.nanoTime();
        while(running){
            long now = System.nanoTime();
            double deltaTime = (now - last) / 1_000_000_000.0;
            last = now;

            runnable.accept(deltaTime);

            while(System.nanoTime() - last < frameTime){
                Thread.onSpinWait();
            }
        }
    }

    public static void main(String[] args){
        new GameLoop(60, dt -> {
            System.out.println("dt = "+dt);
            System.out.println("fps = " + 1/dt);
            System.out.println();
        }).start();
    }
}