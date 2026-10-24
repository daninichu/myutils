package com.daninichu.util;

import java.util.Objects;
import java.util.function.DoubleConsumer;

/**
 * Runs a game update callback at a target frequency,
 * providing the elapsed time between consecutive updates in seconds.
 */
public class GameLoop implements Runnable{
    private final long frameTime;
    private final DoubleConsumer update;
    private Thread thread;
    private volatile boolean running;

    /**
     * Creates a game loop with the specified target update frequency.
     * @param fps target number of updates per second.
     * @param update callback executed on each update,
     *        receiving the elapsed time in seconds since the previous update.
     * @throws IllegalArgumentException if {@code fps} is not positive.
     * @throws NullPointerException if {@code update} is {@code null}.
     */
    public GameLoop(int fps, DoubleConsumer update){
        if(fps <= 0)
            throw new IllegalArgumentException("fps must be positive");
        frameTime = 1_000_000_000 / fps;
        this.update = Objects.requireNonNull(update);
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
            long nextFrame = last + frameTime;
            while(nextFrame - System.nanoTime() > 2_000_000){
                try{
                    Thread.sleep(1);
                } catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            long now;
            while((now = System.nanoTime()) < nextFrame);

            double deltaTime = (now - last) / 1_000_000_000.0;
            last = now;
            update.accept(deltaTime);
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